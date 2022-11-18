package com.woleapp.netpos.contactless.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.FragmentNewOrExistingBinding
import com.woleapp.netpos.contactless.model.AccountNumberLookUpResponse
import com.woleapp.netpos.contactless.model.Status
import com.woleapp.netpos.contactless.ui.dialog.LoadingDialog
import com.woleapp.netpos.contactless.util.AppConstants.STRING_ACCOUNT_NUMBER_LOOKUP_TAG
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.observeServerResponse
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.showSnackBar
import com.woleapp.netpos.contactless.util.Resource
import com.woleapp.netpos.contactless.viewmodels.ContactlessRegViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.dialog_account_number_layout.view.*

@AndroidEntryPoint
class NewOrExistingFragment : BaseFragment() {

    private lateinit var binding: FragmentNewOrExistingBinding
    private val viewModel by activityViewModels<ContactlessRegViewModel>()
    private lateinit var loader:LoadingDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_new_or_existing, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //binding.newOrExistingCardview.visibility = View.VISIBLE
        // binding.newOrExistingCardview.visibility = View.VISIBLE

        loader = LoadingDialog()
        binding.confirmationTvNo.setOnClickListener {
            showFragment(
                RegisterFragment(),
                containerViewId = R.id.auth_container,
                fragmentName = "Register Fragment"
            )
        }

        binding.confirmationTvYes.setOnClickListener {
            //binding.newOrExistingCardview.visibility = View.GONE
            activity?.getFragmentManager()?.popBackStack()
            val dialogView: View = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_account_number_layout, null)
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
            dialogBuilder.setView(dialogView)

            val alertDialog: AlertDialog = dialogBuilder.create()
            alertDialog.show()

            dialogView.dialog_account_number_proceed.setOnClickListener {
                alertDialog.dismiss()
                val account = dialogView.dialog_account_number_editText.text.toString()
                if (account.isNullOrEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Please, enter your account number",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (account.length < 10) {
                    Toast.makeText(
                        requireContext(),
                        "Please, enter valid account number",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showFragment(
                        RegistrationOTPFragment(),
                        containerViewId = R.id.auth_container,
                        fragmentName = "RegisterOTP Fragment"
                    )
//                    viewModel.accountLookUp(account)
//                    observeServerResponse(viewModel.accountNumberResponse, loader, requireActivity().supportFragmentManager){
//                        showFragment(
//                            RegistrationOTPFragment(),
//                            containerViewId = R.id.auth_container,
//                            fragmentName = "RegisterOTP Fragment"
//                        )
//                    }
                }
            }
        }
    }
}


