package com.woleapp.netpos.contactless.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.BuildConfig
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.FragmentDisplayQrBinding
import com.woleapp.netpos.contactless.model.PayWithQrRequest
import com.woleapp.netpos.contactless.model.User
import com.woleapp.netpos.contactless.util.*
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.alertDialog
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.observeServerResponse
import com.woleapp.netpos.contactless.viewmodels.ContactlessQrPaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class DisplayQrFragment : BaseFragment() {

    private lateinit var binding: FragmentDisplayQrBinding
    private var netposID: String? = null
    private var userTID: String? = null
    private var email: String? = null
    private var name: String? = null
    private lateinit var amount: TextInputEditText
    private val viewModel by activityViewModels<ContactlessQrPaymentViewModel>()
    private lateinit var loader: AlertDialog
    private lateinit var partnerID: String

    @Inject
    lateinit var compositeDisposable: CompositeDisposable

    @Inject
    @Named("io-scheduler")
    lateinit var ioScheduler: Scheduler

    @Inject
    @Named("main-scheduler")
    lateinit var mainThreadScheduler: Scheduler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_display_qr, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initPartnerID()
        val user = Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        netposID = user.netplus_id
        userTID = user.terminal_id
        name = user.business_name
        email = "contactless@gmail.com"

        loader = alertDialog(requireContext())

        binding.process.setOnClickListener {
            register()
        }
    }

    private fun initViews() {
        with(binding) {
            amount = priceTextBox
        }
    }

    private fun register() {

        when {
            amount.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_amount))
            }
            else -> {
                if (validateSignUpFieldsOnTextChange()) {
                    registerExistingCustomer()
                }
            }
        }
    }

    private fun validateSignUpFieldsOnTextChange(): Boolean {
        var isValidated = true

        amount.doOnTextChanged { _, _, _, _ ->
            when {
                amount.text.toString().trim().isEmpty() -> {
                    showToast(getString(R.string.all_please_enter_amount))
                    isValidated = false
                }
                else -> {
                    binding.priceInputLayout.error = null
                    isValidated = true
                }
            }
        }
        return isValidated
    }

    private fun registerExistingCustomer() {
        loader.show()
        val paymentWithQr = PayWithQrRequest(
            terminalId = userTID.toString(),
            netposId = netposID.toString(),
            amount = amount.text.toString(),
            name = name.toString(),
            email = email.toString(),
            bank = partnerID
        )

        observeServerResponse(
            viewModel.paymentWithQr(paymentWithQr),
            loader,
            compositeDisposable,
            ioScheduler,
            mainThreadScheduler,
        ) {
            showFragment(
                DisplayQrResultFragment(),
                containerViewId = R.id.container_main,
                fragmentName = "Display QR Result"
            )
        }
    }

    private fun initPartnerID() {
        val bankList = mapOf("firstbank" to "firstbank", "netpos" to "netpos", "fcmb" to "fcmb",
            "easypay" to "fcmb","fcmbeasypay" to "fcmb",
            "providus" to "providus", "providussoftpos" to "providus", "wemabank" to "wemabank", "zenith" to "zenith")

        for (element in bankList) {
            if (element.key == BuildConfig.FLAVOR){
                partnerID = element.value
            }
        }
    }
}