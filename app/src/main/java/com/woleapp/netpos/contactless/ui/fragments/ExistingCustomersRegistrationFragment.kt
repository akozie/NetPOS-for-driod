package com.woleapp.netpos.contactless.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.textfield.TextInputEditText
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.FragmentExisitingCustomersRegistrationBinding
import com.woleapp.netpos.contactless.model.*
import com.woleapp.netpos.contactless.ui.dialog.LoadingDialog
import com.woleapp.netpos.contactless.util.AppConstants
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.showSnackBar
import com.woleapp.netpos.contactless.util.Resource
import com.woleapp.netpos.contactless.viewmodels.ContactlessRegViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExistingCustomersRegistrationFragment : BaseFragment() {

    private lateinit var binding : FragmentExisitingCustomersRegistrationBinding
    private val viewModel by activityViewModels<ContactlessRegViewModel>()
    private lateinit var loader: LoadingDialog
    private lateinit var businessNameView : TextInputEditText
    private lateinit var contactName :TextInputEditText
    private lateinit var addressView : TextInputEditText
    private lateinit var emailView : TextInputEditText
    private lateinit var passwordView :TextInputEditText
    private lateinit var phoneNumber : TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,R.layout.fragment_exisiting_customers_registration,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loader = LoadingDialog()
        initViews()


         val existingAccountRegReq = ExistingAccountRegisterRequest(
            accountNumber = "0123456789",
            businessAddress = addressView.text.toString().trim(),
            businessName = businessNameView.text.toString().trim(),
            contactInformation = contactName.text.toString().trim(),
            email = emailView.text.toString().trim(),
            password = passwordView.text.toString().trim(),
            phoneNumber = phoneNumber.text.toString().trim(),
             terminalId = "",
             merchantId = ""
        )
        viewModel.registerExistingAccount(existingAccountRegReq, 0)
        viewModel.existingRegRequestResponse.observe(viewLifecycleOwner, Observer {
            registerAccount(it){
                showFragment(
                    LoginFragment(),
                    containerViewId = R.id.auth_container,
                    fragmentName = "RegisterOTP Fragment"
                )
            }
        })
    }

    private fun registerAccount(
        existingAccountResp: Resource<ExistingAccountRegisterResponse>,
        successAction: () -> Unit
    ) {
        when (existingAccountResp.status) {
            Status.SUCCESS -> {
                loader.dismiss()
                existingAccountResp.data?.let { accountLookUpResponse ->
                        Toast.makeText(requireContext(), "${existingAccountResp.data.message}", Toast.LENGTH_SHORT).show()
                        successAction()
                }
            }
            Status.LOADING -> {
                loader.show(requireActivity().supportFragmentManager,
                    AppConstants.STRING_ACCOUNT_NUMBER_LOOKUP_TAG
                )
            }
            Status.ERROR -> {
                loader.dismiss()
                showSnackBar(binding.root, getString(R.string.an_error_occurred))
            }
            Status.TIMEOUT -> {
                loader.dismiss()
                showSnackBar(binding.root, getString(R.string.timeOut))
            }
        }
    }

    private fun initViews(){
        with(binding){
            businessNameView = businessName
            contactName = contactInfo
            addressView = address
            emailView = email
            passwordView = password
            phoneNumber = phone
        }
    }
}