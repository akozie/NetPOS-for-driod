package com.woleapp.netpos.contactless.ui.fragments

import android.app.AlertDialog
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
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.chaos.view.PinView
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.BuildConfig
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.FragmentRegistrationOTPBinding
import com.woleapp.netpos.contactless.model.QrTransactionResponseModel
import com.woleapp.netpos.contactless.model.VerveTransactionResponse
import com.woleapp.netpos.contactless.ui.dialog.LoadingDialog
import com.woleapp.netpos.contactless.util.AppConstants
import com.woleapp.netpos.contactless.util.RandomPurposeUtil
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.alertDialog
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.observeServerResponse
import com.woleapp.netpos.contactless.util.Singletons
import com.woleapp.netpos.contactless.viewmodels.ContactlessRegViewModel
import timber.log.Timber

class RegistrationOTPFragment : BaseFragment() {
    private lateinit var binding: FragmentRegistrationOTPBinding
    private lateinit var resendCode: TextView
    private lateinit var otpView: PinView
    private lateinit var otpResentConfirmationText: TextView
    private val viewModel by activityViewModels<ContactlessRegViewModel>()
    private lateinit var loader:AlertDialog
    private lateinit var newAccountNumber:String
    private lateinit var partnerID:String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_registration_o_t_p, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPartnerID()
        viewModel.otpMessage.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
        val newPoneNumber =
            Prefs.getString(AppConstants.SAVE_PHONE_NUMBER, "")
        //val phoneNumber = newPoneNumber.replace("^\"|\"$", "")
        val phoneNumber = newPoneNumber.substring(1, newPoneNumber.length-1)

        val newActNumber =
            Prefs.getString(AppConstants.SAVED_ACCOUNT_NUM_SIGNED_UP, "")
        newAccountNumber =  newActNumber.substring(1, newActNumber.length-1)

        //Timber.d("GGGGGGGGG---->${phoneNumber}")
        loader = alertDialog(requireContext())
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
                        RandomPurposeUtil.closeSoftKeyboard(requireContext(), requireActivity())
                        if (BuildConfig.FLAVOR.contains("providus")){
                            viewModel.confirmOTP("", newAccountNumber, s.toString(), partnerID)
                            observeServerResponse(viewModel.confirmOTPResponse, loader, requireActivity().supportFragmentManager){
                                showFragment(
                                    ExistingCustomersRegistrationFragment(),
                                    containerViewId = R.id.auth_container,
                                    fragmentName = "Register Existing Customer Fragment"
                                )
                            }
                        }else{
                            viewModel.confirmOTP(phoneNumber, newAccountNumber, s.toString(), "")
                            observeServerResponse(viewModel.confirmOTPResponse, loader, requireActivity().supportFragmentManager){
                                showFragment(
                                    ExistingCustomersRegistrationFragment(),
                                    containerViewId = R.id.auth_container,
                                    fragmentName = "Register Existing Customer Fragment"
                                )
                            }
                        }
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {
            }
        })
//        val resendCodeText = "Didn't Receive Code? Resend it" /*getString(R.string.resend_code)*/
//        val spannableText = RandomPurposeUtil.customSpannableString(
//            resendCodeText,
//            resendCodeText.indexOf("Resend it"),
//            resendCodeText.length
//        ) {
//            val qrDataToResend = viewModel.accountLookUp()
//            qrDataToResend?.let {
//                viewModel.setScannedQrIsVerveCard(true)
//                viewModel.postScannedQrRequestToServer(it)
//                observeServerResponse(
//                    viewModel.sendQrToServerResponse,
//                    LoadingDialog(),
//                    requireActivity().supportFragmentManager
//                ) {
//                    otpResentConfirmationText.visibility = View.VISIBLE
//                }
//            }
//        }
//        resendCode.text = spannableText
//        resendCode.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun initViews() {
        with(binding) {
            resendCode = resendOtp
            otpView = pinView
            otpResentConfirmationText = otpResent
        }
    }


    private fun initPartnerID() {
        val bankList = mapOf("firstbank" to "7D66B7F7-222B-41CC-A868-185F3A86313F", "fcmb" to "1B0E68FD-7676-4F2C-883D-3931C3564190", "providus" to "8B26F328-040F-4F27-A5BC-4414AB9D1EFA",
            "wemabank" to "1E3D050B-6995-495F-982A-0511114959C8", "zenith" to "3D9B3E2D-5171-4D6A-99CC-E2799D16DD56")

        for (element in bankList) {
            if (element.key == BuildConfig.FLAVOR)
                partnerID = element.value
            //Timber.d("CODEBANK---->${element.value}")

        }
    }

}