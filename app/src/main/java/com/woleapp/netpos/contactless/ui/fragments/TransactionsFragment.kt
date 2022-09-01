package com.woleapp.netpos.contactless.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.danbamitale.epmslib.entities.TransactionType
import com.google.gson.Gson
import com.netpluspay.netposbarcodesdk.RESULT_CODE_TEXT
import com.woleapp.netpos.contactless.BuildConfig
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.adapter.ServiceAdapter
import com.woleapp.netpos.contactless.databinding.FragmentTransactionsBinding
import com.woleapp.netpos.contactless.databinding.LayoutPreauthDialogBinding
import com.woleapp.netpos.contactless.databinding.LayoutVerveCardQrAmountDialogBinding
import com.woleapp.netpos.contactless.databinding.QrAmoutDialogBinding
import com.woleapp.netpos.contactless.model.PostQrToServerModel
import com.woleapp.netpos.contactless.model.QrCardReadResultModel
import com.woleapp.netpos.contactless.model.QrScannedDataModel
import com.woleapp.netpos.contactless.model.Service
import com.woleapp.netpos.contactless.ui.dialog.LoadingDialog
import com.woleapp.netpos.contactless.util.AppConstants.STRING_QR_READ_RESULT_BUNDLE_KEY
import com.woleapp.netpos.contactless.util.AppConstants.STRING_QR_READ_RESULT_REQUEST_KEY
import com.woleapp.netpos.contactless.util.AppConstants.getGUID
import com.woleapp.netpos.contactless.util.HISTORY_ACTION_PREAUTH
import com.woleapp.netpos.contactless.util.HISTORY_ACTION_REFUND
import com.woleapp.netpos.contactless.util.HISTORY_ACTION_REVERSAL
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.observeServerResponse
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.stringToBase64
import com.woleapp.netpos.contactless.viewmodels.ScanQrViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TransactionsFragment @Inject constructor() : BaseFragment() {

    private lateinit var adapter: ServiceAdapter
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private val scanQrViewModel by activityViewModels<ScanQrViewModel>()
    private lateinit var qrAmoutDialogBinding: QrAmoutDialogBinding
    private lateinit var verveCardQrAmoutDialogBinding: LayoutVerveCardQrAmountDialogBinding
    private lateinit var qrAmountDialog: androidx.appcompat.app.AlertDialog
    private lateinit var qrAmountDialogForVerveCard: androidx.appcompat.app.AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().supportFragmentManager.setFragmentResultListener(
            STRING_QR_READ_RESULT_REQUEST_KEY,
            requireActivity()
        ) { _, bundle ->
            val data = bundle.getParcelable<QrScannedDataModel>(STRING_QR_READ_RESULT_BUNDLE_KEY)
            data?.let {
                if (it.card_scheme.contains(
                        "verve",
                        true
                    )
                ) showAmountDialogForVerveCard(it) else showAmountDialog(it)
            }
        }

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    data?.let {
                        if (it.hasExtra(RESULT_CODE_TEXT)) {
                            val text = it.getStringExtra(RESULT_CODE_TEXT)
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.qr_scanned),
                                Toast.LENGTH_SHORT
                            ).show()
                            text?.let { qrCardData ->
                                val qrReadResult =
                                    Gson().fromJson(qrCardData, QrCardReadResultModel::class.java)
                                val scannedData =
                                    QrScannedDataModel(card_scheme = "", qrReadResult.data)
//                                showAmountDialog(qrReadResult.data)
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "scan failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        adapter = ServiceAdapter {
            val nextFrag: Fragment? = when (it.id) {
                0 -> SalesFragment.newInstance()
                1 -> TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REFUND)
                8 -> TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REVERSAL)
                7 -> SalesFragment.newInstance(TransactionType.PURCHASE_WITH_CASH_BACK)
                2 -> {
                    showPreAuthDialog()
                    null
                }
                4 ->
                    FragmentBarCodeScanner()

//                {
// //                     NetPosBarcodeSdk
//                    // QRFragment()
//                    NetPosBarcodeSdk.startScan(requireContext(), resultLauncher)
// //                    addFragmentWithoutRemove(FragmentBarCodeScanner())
//                    null
//                }
                5 -> ReprintFragment()
                6 -> SalesFragment.newInstance(isVend = true)
                else -> SalesFragment.newInstance(TransactionType.CASH_ADVANCE)
            }
            nextFrag?.let { fragment ->
                addFragmentWithoutRemove(fragment)
            }
        }

        qrAmoutDialogBinding = QrAmoutDialogBinding.inflate(inflater, null, false)
            .apply {
                executePendingBindings()
                lifecycleOwner = viewLifecycleOwner
            }

        verveCardQrAmoutDialogBinding =
            LayoutVerveCardQrAmountDialogBinding.inflate(inflater, null, false)
                .apply {
                    executePendingBindings()
                    lifecycleOwner = viewLifecycleOwner
                }

        qrAmountDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext()).apply {
            setView(qrAmoutDialogBinding.root)
        }.create()

        qrAmountDialogForVerveCard =
            androidx.appcompat.app.AlertDialog.Builder(requireContext()).apply {
                setView(verveCardQrAmoutDialogBinding.root)
            }.create()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTransactions.layoutManager = GridLayoutManager(context, 2)
        binding.rvTransactions.adapter = adapter
        setService()
    }

    private fun setService() {
        val listOfService = ArrayList<Service>()
            .apply {
                add(Service(0, "Purchase", R.drawable.ic_purchase))
                add(Service(7, "Purchase With Cashback", R.drawable.ic_purchase))
                // add(Service(1, "Refund", R.drawable.ic_loop))
                // add(Service(8, "Reversal", R.drawable.ic_loop))
                // add(Service(2, "PRE AUTHORIZATION", R.drawable.ic_pre_auth))
                add(Service(3, "Cash Advance", R.drawable.ic_pay_cash_icon))
                add(Service(4, "Scan QR", R.drawable.ic_qr_code))
                add(Service(5, "Reprint", R.drawable.ic_print))
                // add(Service(6, "VEND", R.drawable.ic_vend))
            }
        adapter.submitList(listOfService)
    }

    private fun showPreAuthDialog() {
        val dialog = AlertDialog.Builder(context)
            .apply {
                setCancelable(false)
            }.create()
        val preAuthDialogBinding =
            LayoutPreauthDialogBinding.inflate(LayoutInflater.from(context), null, false)
                .apply {
                    lifecycleOwner = viewLifecycleOwner
                    executePendingBindings()
                    preAuthNew.setOnClickListener {
                        dialog.dismiss()
                        addFragmentWithoutRemove(SalesFragment.newInstance(TransactionType.PRE_AUTHORIZATION))
                    }
                    preAuthComplete.setOnClickListener {
                        dialog.dismiss()
                        addFragmentWithoutRemove(
                            TransactionHistoryFragment.newInstance(
                                HISTORY_ACTION_PREAUTH
                            )
                        )
                    }
                    cancelButton.setOnClickListener {
                        dialog.dismiss()
                    }
                }
        dialog.setView(preAuthDialogBinding.root)
        dialog.show()
    }

    private fun showAmountDialog(qrData: QrScannedDataModel) {
        qrAmountDialog.show()
        qrAmoutDialogBinding.proceed.setOnClickListener {
            val amountDouble = qrAmoutDialogBinding.amount.text.toString().toDoubleOrNull()
            if (qrAmoutDialogBinding.amount.text.isNullOrEmpty()) qrAmoutDialogBinding.amount.error =
                getString(R.string.amount_empty)
            amountDouble?.let {
                qrAmoutDialogBinding.amount.text?.clear()
                qrAmountDialog.cancel()
                val qrDataToSendToBackend =
                    PostQrToServerModel(
                        it,
                        qrData.data,
                        merchantId = BuildConfig.STRING_MERCHANT_ID
                    )
                scanQrViewModel.setScannedQrIsVerveCard(false)
                scanQrViewModel.postScannedQrRequestToServer(qrDataToSendToBackend)
                observeServerResponse(
                    scanQrViewModel.sendQrToServerResponse,
                    LoadingDialog(),
                    childFragmentManager
                ) {
                    addFragmentWithoutRemove(
                        CompleteQrPaymentWebViewFragment()
                    )
                }
            }
        }
    }

    private fun showAmountDialogForVerveCard(qrData: QrScannedDataModel) {
        qrAmountDialogForVerveCard.show()
        verveCardQrAmoutDialogBinding.proceed.setOnClickListener {
            if (verveCardQrAmoutDialogBinding.amount.text.isNullOrEmpty()) {
                verveCardQrAmoutDialogBinding.amount.error = getString(R.string.amount_empty)
            }
            if (verveCardQrAmoutDialogBinding.pin.text.isNullOrEmpty()) {
                verveCardQrAmoutDialogBinding.pin.error = getString(R.string.enter_pin)
            }
            val amountDouble = verveCardQrAmoutDialogBinding.amount.text.toString().toDoubleOrNull()
            verveCardQrAmoutDialogBinding.pin.text.toString().trim().let { pin ->
                val formattedPadding = stringToBase64(pin).removeSuffix('\n'.toString())
                if (pin.length == 4) {
                    amountDouble?.let {
                        verveCardQrAmoutDialogBinding.amount.text?.clear()
                        verveCardQrAmoutDialogBinding.pin.text?.clear()
                        qrAmountDialogForVerveCard.cancel()
                        val qrDataToSendToBackend =
                            PostQrToServerModel(
                                it,
                                qrData.data,
                                merchantId = BuildConfig.STRING_MERCHANT_ID,
                                padding = formattedPadding
                            )
                        scanQrViewModel.setScannedQrIsVerveCard(true)
                        scanQrViewModel.saveTheQrToSharedPrefs(qrDataToSendToBackend.copy(orderId = getGUID()))
                        scanQrViewModel.postScannedQrRequestToServer(qrDataToSendToBackend)
                        observeServerResponse(
                            scanQrViewModel.sendQrToServerResponseVerve,
                            LoadingDialog(),
                            requireActivity().supportFragmentManager
                        ) {
                            addFragmentWithoutRemove(
                                EnterOtpFragment()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
