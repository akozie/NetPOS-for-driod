package com.woleapp.netpos.contactless.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.chaos.view.PinView
import com.google.gson.Gson
import com.woleapp.netpos.contactless.BuildConfig
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.LayoutEnterOtpFragmentBinding
import com.woleapp.netpos.contactless.model.QrTransactionResponseModel
import com.woleapp.netpos.contactless.model.VerveTransactionResponse
import com.woleapp.netpos.contactless.ui.dialog.LoadingDialog
import com.woleapp.netpos.contactless.ui.dialog.ResponseModal
import com.woleapp.netpos.contactless.util.AppConstants.QR_TRANSACTION_RESULT_BUNDLE_KEY
import com.woleapp.netpos.contactless.util.AppConstants.QR_TRANSACTION_RESULT_REQUEST_KEY
import com.woleapp.netpos.contactless.util.AppConstants.STRING_QR_RESPONSE_MODAL_DIALOG_TAG
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.closeSoftKeyboard
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.customSpannableString
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.formatFailedVerveTransRespToExtractIswResponse
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.observeServerResponse
import com.woleapp.netpos.contactless.util.Singletons
import com.woleapp.netpos.contactless.viewmodels.ScanQrViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EnterOtpFragment @Inject constructor() : BaseFragment() {
    private lateinit var resendCode: TextView
    private lateinit var otpView: PinView
    private lateinit var otpResentConfirmationText: TextView
    private lateinit var binding: LayoutEnterOtpFragmentBinding

    @Inject
    lateinit var responseModal: ResponseModal

    @Inject
    lateinit var gson: Gson
    private val viewModel by activityViewModels<ScanQrViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.layout_enter_otp_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        otpView.requestFocus()
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
        otpView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    if (it.length == 6) {
                        closeSoftKeyboard(requireContext(), requireActivity())
                        viewModel.sendOtpForVerveCard(s.toString())
                        observeServerResponse(
                            viewModel.transactionResponseFromVerve,
                            LoadingDialog(),
                            requireActivity().supportFragmentManager
                        ) {
                            val transactionResponseFromVerve =
                                viewModel.transactionResponseFromVerve.value!!.data!!
                            val transResp: QrTransactionResponseModel = try {
                                formatFailedVerveTransRespToExtractIswResponse(
                                    transactionResponseFromVerve as VerveTransactionResponse
                                ).mapToQrTransactionResponseModel()
                            } catch (e: Exception) {
                                (transactionResponseFromVerve as VerveTransactionResponse).mapToQrTransactionResponseModel()
                            }

                            val formattedTransactionResp =
                                transResp.mapQrTransRespToQrRespFinalModel(
                                    "",
                                    terminalId = Singletons.getCurrentlyLoggedInUser()?.terminal_id
                                        ?: "",
                                    merchantId = BuildConfig.STRING_MERCHANT_ID
                                )
                            requireActivity().supportFragmentManager.setFragmentResult(
                                QR_TRANSACTION_RESULT_REQUEST_KEY,
                                bundleOf(QR_TRANSACTION_RESULT_BUNDLE_KEY to formattedTransactionResp)
                            )
                            requireActivity().supportFragmentManager.popBackStack()
                            responseModal.show(
                                requireActivity().supportFragmentManager,
                                STRING_QR_RESPONSE_MODAL_DIALOG_TAG
                            )
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
        val resendCodeText = "Didn't Receive Code? Resend it" /*getString(R.string.resend_code)*/
        val spannableText = customSpannableString(
            resendCodeText,
            resendCodeText.indexOf("Resend it"),
            resendCodeText.length
        ) {
            val qrDataToResend = viewModel.retrieveQrPostPayloadFromSharedPrefs()
            qrDataToResend?.let {
                viewModel.setScannedQrIsVerveCard(true)
                viewModel.postScannedQrRequestToServer(it)
                observeServerResponse(
                    viewModel.sendQrToServerResponse,
                    LoadingDialog(),
                    requireActivity().supportFragmentManager
                ) {
                    otpResentConfirmationText.visibility = View.VISIBLE
                }
            }
        }
        resendCode.text = spannableText
        resendCode.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun initViews() {
        with(binding) {
            resendCode = resendOtp
            otpView = pinView
            otpResentConfirmationText = otpResent
        }
    }
}
