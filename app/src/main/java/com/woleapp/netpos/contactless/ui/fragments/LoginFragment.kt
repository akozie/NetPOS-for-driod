package com.woleapp.netpos.contactless.ui.fragments

import android.content.Intent
import android.nfc.NfcManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.google.gson.JsonObject
import com.woleapp.netpos.contactless.BuildConfig
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.DialogPasswordResetBinding
import com.woleapp.netpos.contactless.databinding.FragmentLoginBinding
import com.woleapp.netpos.contactless.model.AuthenticationEventData
import com.woleapp.netpos.contactless.model.MqttEvent
import com.woleapp.netpos.contactless.model.MqttEvents
import com.woleapp.netpos.contactless.network.StormApiClient
import com.woleapp.netpos.contactless.nibss.NetPosTerminalConfig
import com.woleapp.netpos.contactless.ui.activities.MainActivity
import com.woleapp.netpos.contactless.viewmodels.AuthViewModel

class LoginFragment : BaseFragment() {

    private val viewModel by viewModels<AuthViewModel>()
    private lateinit var binding: FragmentLoginBinding
    private lateinit var resetPasswordBinding: DialogPasswordResetBinding
    private lateinit var passwordResetDialog: AlertDialog
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            executePendingBindings()
            viewmodel = viewModel
        }
        resetPasswordBinding = DialogPasswordResetBinding.inflate(inflater, null, false)
            .apply {
                lifecycleOwner = viewLifecycleOwner
                executePendingBindings()
                viewmodel = viewModel
            }
        passwordResetDialog = AlertDialog.Builder(requireContext())
            .apply {
                setView(resetPasswordBinding.root)
                setCancelable(false)
            }.create()
        resetPasswordBinding.closeDialog.setOnClickListener {
            passwordResetDialog.cancel()
        }
        val credentials = JsonObject()
        credentials.addProperty("appname", getString(R.string._app_name))
        credentials.addProperty("password", getString(R.string._password))
        viewModel.apply {
            stormApiService = StormApiClient.getInstance()
            appCredentials = credentials
        }
        binding.btnLogin.setOnClickListener {
            viewModel.login()
        }

        binding.register.setOnClickListener {
            if (BuildConfig.FLAVOR.contains("firstbank")||BuildConfig.FLAVOR.contains("providus")||
                BuildConfig.FLAVOR.contains("wemabank")||BuildConfig.FLAVOR.contains("fcmb")||
                BuildConfig.FLAVOR.contains("zenith")){
                showFragment(
                    NewOrExistingFragment(),
                    containerViewId = R.id.auth_container,
                    fragmentName = "NewOrExisting Fragment"
                )
            }else{
                showFragment(
                    RegisterFragment(),
                    containerViewId = R.id.auth_container,
                    fragmentName = "Register Fragment"
                )
            }
        }

        binding.testMyPhone.setOnClickListener {
            testNfcSupport()
        }
        binding.contactlessLogo.setOnClickListener {
            testNfcSupport()
        }
        return binding.root
    }

    private fun testNfcSupport() {
        val nfcManager: NfcManager =
            requireActivity().getSystemService(AppCompatActivity.NFC_SERVICE) as NfcManager
        val nfcAdapter = nfcManager.defaultAdapter
        if (nfcAdapter != null) {
            // Toast.makeText(this, "Device has NFC support", Toast.LENGTH_SHORT).show()
            if (nfcAdapter.isEnabled) {
                Toast.makeText(requireContext(), "NFC enabled", Toast.LENGTH_SHORT).show()
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("NFC Message")
                    .setMessage("Device has NFC support and is enabled")
                    .setPositiveButton("Close") { dialog, _ ->
                        dialog.dismiss()
                        // finish()
                    }.show()
            } else {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("NFC Message")
                    .setMessage("NFC is not enabled, goto device settings to enable")
                    .setPositiveButton("Settings") { dialog, _ ->
                        dialog.dismiss()
                        startActivityForResult(
                            Intent(android.provider.Settings.ACTION_NFC_SETTINGS),
                            0
                        )
                    }.show()
            }
        } else {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("NFC Message")
                .setMessage("Device dose not have NFC support")
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                    // finish()
                }.show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.message.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
        binding.forgotPassword.setOnClickListener {
            passwordResetDialog.show()
        }
        viewModel.authDone.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { authenticated ->
                if (authenticated) {
                    activity?.apply {
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                        NetPosTerminalConfig.init(applicationContext)
                        val event = MqttEvent<AuthenticationEventData>().apply {
                            this.event = MqttEvents.AUTHENTICATION.event
                            this.code = "00"
                            this.timestamp = System.currentTimeMillis()
                            this.data = AuthenticationEventData(
                                this.business_name!!,
                                this.storm_id!!,
                                this.deviceSerial!!
                            )
                            this.status = "SUCCESS"
                        }
                        // MqttHelper.init(applicationContext, event, MqttTopics.AUTHENTICATION)
                        finish()
                    }
                }
            }
        }
        viewModel.passwordResetSent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it && passwordResetDialog.isShowing) {
                    passwordResetDialog.cancel()
                }
            }
        }
    }
}
