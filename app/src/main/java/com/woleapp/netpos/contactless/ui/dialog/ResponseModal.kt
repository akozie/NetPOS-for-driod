package com.woleapp.netpos.contactless.ui.dialog

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import com.airbnb.lottie.LottieAnimationView
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.database.AppDatabase
import com.woleapp.netpos.contactless.databinding.LayoutQrReceiptPdfBinding
import com.woleapp.netpos.contactless.databinding.TransactionStatusModalBinding
import com.woleapp.netpos.contactless.model.ModalData
import com.woleapp.netpos.contactless.model.QrTransactionResponseFinalModel
import com.woleapp.netpos.contactless.util.AppConstants.QR_TRANSACTION_RESULT_BUNDLE_KEY
import com.woleapp.netpos.contactless.util.AppConstants.QR_TRANSACTION_RESULT_REQUEST_KEY
import com.woleapp.netpos.contactless.util.AppConstants.WRITE_PERMISSION_REQUEST_CODE
import com.woleapp.netpos.contactless.util.Mappers.mapQrTransToNormalTransRespType
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.showSnackBar
import com.woleapp.netpos.contactless.util.createPdf
import com.woleapp.netpos.contactless.util.genericPermissionHandler
import com.woleapp.netpos.contactless.util.initViewsForPdfLayout
import com.woleapp.netpos.contactless.util.sharePdf
import com.woleapp.netpos.contactless.viewmodels.NfcCardReaderViewModel
import com.woleapp.netpos.contactless.viewmodels.ScanQrViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ResponseModal @Inject constructor() : DialogFragment() {
    private lateinit var binding: TransactionStatusModalBinding
    private lateinit var lottieIcon: LottieAnimationView
    private lateinit var statusTv: TextView
    private lateinit var cancelBtn: ImageView
    private lateinit var sendReceiptAsSms: ImageView
    private lateinit var downloadReceiptAsPdf: ImageView
    private lateinit var shareReceiptAsPdf: ImageView
    private lateinit var amountTv: TextView
    private var modalData: ModalData? = null
    private lateinit var pdfView: LayoutQrReceiptPdfBinding
    private lateinit var receiptPdf: File
    private var responseFromWebView: QrTransactionResponseFinalModel? = null
    private val scanQrViewModel by activityViewModels<ScanQrViewModel>()
    private val nfcCardReaderViewModel by activityViewModels<NfcCardReaderViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(QR_TRANSACTION_RESULT_REQUEST_KEY) { _, bundle ->
            responseFromWebView =
                bundle.getParcelable(QR_TRANSACTION_RESULT_BUNDLE_KEY)
            modalData =
                if (responseFromWebView is QrTransactionResponseFinalModel && responseFromWebView != null) {
                    saveTransactionIntoDb(responseFromWebView!!)
                    nfcCardReaderViewModel.setQrTransactionResponse(responseFromWebView!!)
                    ModalData(
                        responseFromWebView!!.responseCode == "00",
                        responseFromWebView!!.amount.toDouble()
                    )
                } else ModalData(false, 0.0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        pdfView =
            DataBindingUtil.inflate(inflater, R.layout.layout_qr_receipt_pdf, container, false)
        binding =
            DataBindingUtil.inflate(inflater, R.layout.transaction_status_modal, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        dialog?.window?.apply {
            setBackgroundDrawableResource(R.drawable.curve_bg)
            isCancelable = false
        }
        initViewsForPdfLayout(
            pdfView,
            responseFromWebView
        )
    }

    override fun onResume() {
        super.onResume()
        setData()
        cancelBtn.setOnClickListener {
            dialog?.dismiss()
        }
        sendReceiptAsSms.setOnClickListener {
            dialog?.dismiss()
            nfcCardReaderViewModel.showReceiptDialogForQrPayment()
        }
        shareReceiptAsPdf.setOnClickListener {
            initViewsForPdfLayout(pdfView, responseFromWebView)
            dialog?.dismiss()
            receiptPdf = createPdf(pdfView, this)

            sharePdf(receiptPdf, this@ResponseModal)
        }
        downloadReceiptAsPdf.setOnClickListener {
            initViewsForPdfLayout(pdfView, responseFromWebView)
            genericPermissionHandler(
                this@ResponseModal,
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                WRITE_PERMISSION_REQUEST_CODE,
                getString(R.string.storage_permission_rationale_for_download)
            ) {
                receiptPdf = createPdf(pdfView, this)
                showSnackBar(
                    binding.root,
                    getString(R.string.fileDownloaded)
                )
            }
        }
    }

    private fun initViews() {
        with(binding) {
            shareReceiptAsPdf = shareReceipt
            downloadReceiptAsPdf = downloadReceipt
            lottieIcon = statusIconLAV
            statusTv = successFailed
            cancelBtn = cancelButton
            amountTv = qrAmount
            sendReceiptAsSms = printReceipt
        }
    }

    private fun setData() {
        modalData?.let {
//            scanQrViewModel.setQrTransactionResponse()
            if (it.status) {
                lottieIcon.setAnimation(R.raw.lottiesuccess)
                statusTv.text = getString(R.string.success)
                statusTv.setTextColor(resources.getColor(R.color.success))
            } else {
                lottieIcon.setAnimation(R.raw.failed)
                statusTv.text = getString(R.string.failed)
                statusTv.setTextColor(resources.getColor(R.color.failed))
            }
            amountTv.text = it.amount.toLong().formatCurrencyAmount("\u20A6")
        }
    }

    private fun saveTransactionIntoDb(qrTransaction: QrTransactionResponseFinalModel) {
        val qrTransactionWithCardScheme =
            qrTransaction.copy(cardLabel = scanQrViewModel.getCardScheme())
        val transactionResponse = mapQrTransToNormalTransRespType(qrTransactionWithCardScheme)

        AppDatabase.getDatabaseInstance(requireContext()).transactionResponseDao()
            .insertNewTransaction(
                transactionResponse.copy(
                    amount = transactionResponse.amount.times(
                        100
                    )
                )
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { responseFromDbOperations, error ->
                responseFromDbOperations?.let {
                }
                error?.let {
                    Timber.d("DATA_FROM_SAVING_TO_DB====>%s", it.localizedMessage)
                }
            }
    }
}
