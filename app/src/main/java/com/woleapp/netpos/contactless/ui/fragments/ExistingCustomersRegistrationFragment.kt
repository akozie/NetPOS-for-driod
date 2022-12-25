package com.woleapp.netpos.contactless.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.BuildConfig
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.FragmentExisitingCustomersRegistrationBinding
import com.woleapp.netpos.contactless.model.Data
import com.woleapp.netpos.contactless.model.ExistingAccountRegisterRequest
import com.woleapp.netpos.contactless.ui.dialog.LoadingDialog
import com.woleapp.netpos.contactless.util.*
import com.woleapp.netpos.contactless.util.AppConstants.SAVED_ACCOUNT_NUM_SIGNED_UP
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.alertDialog
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.observeServerResponse
import com.woleapp.netpos.contactless.util.Singletons.gson
import com.woleapp.netpos.contactless.viewmodels.ContactlessRegViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.dialog_terms_and_conditions.view.*
import timber.log.Timber

@AndroidEntryPoint
class ExistingCustomersRegistrationFragment : BaseFragment() {

    private lateinit var binding: FragmentExisitingCustomersRegistrationBinding
    private val viewModel by activityViewModels<ContactlessRegViewModel>()
    private lateinit var loader: AlertDialog
    private lateinit var businessNameView: TextInputEditText
    private lateinit var contactName: TextInputEditText
    private lateinit var addressView: TextInputEditText
    private lateinit var emailView: TextInputEditText
    private lateinit var passwordView: TextInputEditText
    private lateinit var confirmPasswordView: TextInputEditText
    private lateinit var phoneNumber: TextInputEditText
    private lateinit var submitBtn: Button
    private lateinit var partnerID: String
    private lateinit var actNumber: String
   // private lateinit var customerData: Data

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_exisiting_customers_registration,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //loader = LoadingDialog()
        viewModel.registerMessage.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
        initViews()
        initPartnerID()
        val newActNumber =
            Prefs.getString(AppConstants.SAVED_ACCOUNT_NUM_SIGNED_UP, "")
        //val phoneNumber = newPoneNumber.replace("^\"|\"$", "")
        actNumber =  newActNumber.substring(1, newActNumber.length-1)

        val newBusinessName =
            Prefs.getString(AppConstants.BUSINESS_NAME, "")
        val businessName = newBusinessName.substring(1, newBusinessName.length-1)

        val newBusinessAddress =
            Prefs.getString(AppConstants.BUSINESS_ADDRESS, "")
        val businessAddress = newBusinessAddress.substring(1, newBusinessAddress.length-1)

        val newEmail =
            Prefs.getString(AppConstants.EMAIL_ADDRESS, "")
        val email = newEmail.substring(1, newEmail.length-1)

        val newPhone =
            Prefs.getString(AppConstants.PHONE_NUMBER, "")
        val phone = newPhone.substring(1, newPhone.length-1)

        val newContactInfo =
            Prefs.getString(AppConstants.FULL_NAME, "")
        val contactInfo = newContactInfo.substring(1, newContactInfo.length-1)


        //Timber.d("VVVVVVVVV--->${customerData}")
        binding.businessName.setText(businessName)
        binding.contactInfo.setText(contactInfo)
        binding.address.setText(businessAddress)
        binding.phone.setText(phone)
        binding.email.setText(email)


        loader = alertDialog(requireContext())

        submitBtn.setOnClickListener {
            register()
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

    private fun initViews() {
        with(binding) {
            businessNameView = businessName
            contactName = contactInfo
            addressView = address
            emailView = email
            passwordView = password
            confirmPasswordView = confirmPassword
            phoneNumber = phone
            submitBtn = btnLogin
        }
    }

    private fun register() {

        when {
            businessNameView.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_business_name))
            }
            contactName.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_full_name))
            }
            addressView.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_location))
            }
            phoneNumber.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_phone_number))
            }
            emailView.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_email_address))
            }
            passwordView.text.toString().isEmpty() -> {
                showToast(getString(R.string.hint_enter_password))
            }
            confirmPasswordView.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_confirm_password))
            }
            !validatePasswordMismatch(
                passwordView.text.toString(),
                confirmPasswordView.text.toString()
            ) -> {
                showToast(getString(R.string.all_password_mismatch))
            }
            else -> {
                if (validateSignUpFieldsOnTextChange()) {
                    if (BuildConfig.FLAVOR.contains("providus")) {
                        activity?.getFragmentManager()?.popBackStack()
                        val dialogView: View = LayoutInflater.from(requireContext())
                            .inflate(R.layout.dialog_terms_and_conditions, null)
                        val dialogBuilder: AlertDialog.Builder =
                            AlertDialog.Builder(requireContext())
                        dialogBuilder.setView(dialogView)

                        val alertDialog: AlertDialog = dialogBuilder.create()
                        alertDialog.show()

                        dialogView.accept_button.setOnClickListener {
                            registerExistingCustomer()
                        }
                    } else {
                        registerExistingCustomer()
                    }
                }
            }
        }
    }

    private fun validateSignUpFieldsOnTextChange(): Boolean {
        var isValidated = true

        businessNameView.doOnTextChanged { _, _, _, _ ->
            when {
                businessNameView.text.toString().trim().isEmpty() -> {
//                    binding.existingCustomersBusinessName.error =
//                        getString(R.string.all_please_enter_business_name)
                    showToast(getString(R.string.all_please_enter_business_name))
                    isValidated = false
                }
                else -> {
                    binding.existingCustomersBusinessName.error = null
                    isValidated = true
                }
            }
        }
        contactName.doOnTextChanged { _, _, _, _ ->
            when {
                contactName.text.toString().trim().isEmpty() -> {
                    showToast(getString(R.string.all_please_enter_full_name))
                    isValidated = false
                }
                else -> {
                    binding.existingCustomersContactInfo.error = null
                    isValidated = true
                }
            }
        }
        addressView.doOnTextChanged { _, _, _, _ ->
            when {
                addressView.text.toString().trim().isEmpty() -> {
                    binding.existingCustomersTextInputAddress.error =
                        getString(R.string.all_please_enter_location)
                    isValidated = false
                }
                else -> {
                    binding.existingCustomersTextInputAddress.error = null
                    isValidated = true
                }
            }
        }
        emailView.doOnTextChanged { _, _, _, _ ->
            when {
                emailView.text.toString().trim().isEmpty() -> {
                    binding.emailField.error =
                        getString(R.string.all_please_enter_email_address)
                    isValidated = false
                }
                else -> {
                    binding.emailField.error = null
                    isValidated = true
                }
            }
        }
        binding.password.doOnTextChanged { _, _, _, _ ->
            when {
                binding.password.text.toString().trim().isEmpty() -> {
                    binding.passwordField.error =
                        getString(R.string.all_please_enter_password)
                    binding.passwordField.errorIconDrawable = null
                    isValidated = false
                }
                else -> {
                    binding.passwordField.error = null
                    isValidated = true
                }
            }
        }
        binding.confirmPassword.doOnTextChanged { _, _, _, _ ->
            when {
                binding.confirmPassword.text.toString().trim().isEmpty() -> {
                    binding.confirmPasswordField.error =
                        getString(R.string.all_please_enter_confirm_password)
                    binding.confirmPasswordField.errorIconDrawable =
                        null
                    isValidated = false
                }
                !validatePasswordMismatch(
                    binding.password.text.toString().trim(),
                    binding.confirmPassword.text.toString().trim()
                ) -> {
                    binding.confirmPasswordField.error =
                        getString(R.string.all_password_mismatch)
                    binding.confirmPasswordField.errorIconDrawable =
                        null
                    isValidated = false
                }
                else -> {
                    binding.confirmPasswordField.error = null
                    isValidated = true
                }
            }
        }
        return isValidated
    }

    private fun registerExistingCustomer() {
        val existingAccountRegReq = ExistingAccountRegisterRequest(
            accountNumber = actNumber,
            businessAddress = addressView.text.toString().trim(),
            businessName = businessNameView.text.toString().trim(),
            contactInformation = contactName.text.toString().trim(),
            username = emailView.text.toString().trim(),
            password = passwordView.text.toString().trim(),
            phoneNumber = phoneNumber.text.toString().trim()
//            terminalId = "2035095W",
//            merchantId = "2035FC190031251"
        )
        viewModel.registerExistingAccount(
            existingAccountRegReq,
            partnerId = partnerID
        )
        observeServerResponse(
            viewModel.existingRegRequestResponse,
            loader,
            requireActivity().supportFragmentManager
        ) {
            showFragment(
                LoginFragment(),
                containerViewId = R.id.auth_container,
                fragmentName = "Login Fragment"
            )
        }
    }
}
