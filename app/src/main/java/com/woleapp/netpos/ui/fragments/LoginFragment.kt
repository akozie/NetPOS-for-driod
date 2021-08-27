package com.woleapp.netpos.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.google.gson.JsonObject
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.DialogPasswordResetBinding
import com.woleapp.netpos.databinding.FragmentLoginBinding
import com.woleapp.netpos.model.AuthenticationEventData
import com.woleapp.netpos.model.MqttEvent
import com.woleapp.netpos.model.MqttEvents
import com.woleapp.netpos.model.MqttTopics
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.ui.activities.MainActivity
import com.woleapp.netpos.viewmodels.AuthViewModel

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
        return binding.root
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
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
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
                        MqttHelper.init(applicationContext, event, MqttTopics.AUTHENTICATION)
                        finish()
                    }
                }
            }
        }

        viewModel.gotoAdminPage.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { gotoAdminPage ->
                if (gotoAdminPage)
                    addFragmentWithoutRemove(AdministratorFragment(), R.id.auth_container)
            }
        }
        viewModel.passwordResetSent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it && passwordResetDialog.isShowing)
                    passwordResetDialog.cancel()
            }
        }
    }
}