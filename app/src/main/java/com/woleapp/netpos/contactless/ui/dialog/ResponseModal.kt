package com.woleapp.netpos.contactless.ui.dialog

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
import com.woleapp.netpos.contactless.util.AppConstants.IS_QR_TRANSACTION
import com.woleapp.netpos.contactless.util.AppConstants.QR_TRANSACTION_RESULT_BUNDLE_KEY
import com.woleapp.netpos.contactless.util.AppConstants.QR_TRANSACTION_RESULT_REQUEST_KEY
import com.woleapp.netpos.contactless.util.AppConstants.SAVE_QR_TRANS_TO_DB
import com.woleapp.netpos.contactless.util.Mappers.mapQrTransToNormalTransRespType
import com.woleapp.netpos.contactless.util.RxJavaUtils.getSingleTransformer
import com.woleapp.netpos.contactless.util.initViewsForPdfLayout
import com.woleapp.netpos.contactless.viewmodels.NfcCardReaderViewModel
import com.woleapp.netpos.contactless.viewmodels.ScanQrViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ResponseModal @Inject constructor() : DialogFragment() {
    private lateinit var binding: TransactionStatusModalBinding
    private lateinit var lottieIcon: LottieAnimationView
    private lateinit var statusTv: TextView
    private lateinit var cancelBtn: ImageView
    private lateinit var sendReceiptAsSms: ImageView
    private lateinit var amountTv: TextView
    private var modalData: ModalData? = null
    private lateinit var pdfView: LayoutQrReceiptPdfBinding
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
            responseFromWebView?.let { qrTransResponse ->
                nfcCardReaderViewModel.setQrTransactionResponse(qrTransResponse)
                nfcCardReaderViewModel.showReceiptDialogForQrPayment()
            }
        }
    }

    private fun initViews() {
        with(binding) {
            lottieIcon = statusIconLAV
            statusTv = successFailed
            cancelBtn = cancelButton
            amountTv = qrAmount
            sendReceiptAsSms = printReceipt
        }
    }

    private fun setData() {
        modalData?.let {
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
                    ),
                    TVR = transactionResponse.TVR + IS_QR_TRANSACTION
                )
            )
            .compose(getSingleTransformer(SAVE_QR_TRANS_TO_DB))
            .subscribe { responseFromDbOperations ->
                responseFromDbOperations?.let {
                }
            }
    }
}
