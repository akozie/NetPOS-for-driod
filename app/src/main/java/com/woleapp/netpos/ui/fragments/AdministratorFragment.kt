@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.woleapp.netpos.databinding.FragmentAdministratorBinding
import com.woleapp.netpos.nibss.CONFIGURATION_ACTION
import com.woleapp.netpos.nibss.CONFIGURATION_STATUS
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.viewmodels.AdministratorViewModel

class AdministratorFragment : Fragment() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.getIntExtra(CONFIGURATION_STATUS, -1)) {
                    0 -> showProgressDialog()
                    1 -> {
                        Toast.makeText(context!!, "Terminal Configured", Toast.LENGTH_LONG).show()
                        dismissProgressDialogIfShowing()
                        viewModel.saveConfigurationToPreference()
                    }
                    -1 -> {
                        dismissProgressDialogIfShowing()
                        showAlertDialog()
                    }
                }
            }
        }

    }
    private var progressDialog: ProgressDialog? = null
    private lateinit var alertDialog: AlertDialog
    private lateinit var binding: FragmentAdministratorBinding
    private val viewModel by viewModels<AdministratorViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdministratorBinding.inflate(inflater, container, false)
            .apply {
                lifecycleOwner = viewLifecycleOwner
                executePendingBindings()
                viewmodel = viewModel
            }
        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Configuring Terminal, Please wait")
            setCancelable(false)
        }
        alertDialog = AlertDialog.Builder(requireContext()).run {
            setCancelable(false)
            setTitle("Message")
            setPositiveButton("Retry") { dialog, _ ->
                NetPosTerminalConfig.init(requireContext())
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                Toast.makeText(requireContext(), "Configuration cancelled", Toast.LENGTH_LONG)
                    .show()
                dialog.dismiss()
            }
            create()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.saveConfigurationData.observe(viewLifecycleOwner) {
            NetPosTerminalConfig.init(requireContext())
        }
    }

    private fun showAlertDialog() {
        alertDialog.apply {
            setMessage("Terminal Configuration Failed")
            show()
        }
    }

    private fun dismissProgressDialogIfShowing() {
        progressDialog?.dismiss()
    }

    private fun showProgressDialog() {
        progressDialog?.show()
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(receiver, IntentFilter(CONFIGURATION_ACTION))
    }
}