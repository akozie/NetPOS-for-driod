@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.DialogTransactionResultBinding
import com.woleapp.netpos.databinding.FragmentTransactionDetailsBinding
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.HISTORY_ACTION_DEFAULT
import com.woleapp.netpos.util.HISTORY_ACTION_PREAUTH
import com.woleapp.netpos.util.builder
import com.woleapp.netpos.util.showCardDialog
import com.woleapp.netpos.viewmodels.TransactionsViewModel
import timber.log.Timber

class TransactionDetailsFragment : BaseFragment() {
    private val viewModel by activityViewModels<TransactionsViewModel>()
    private lateinit var binding: FragmentTransactionDetailsBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var alertDialog: AlertDialog
    private lateinit var receiptDialogBinding: DialogTransactionResultBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTransactionDetailsBinding.inflate(inflater, container, false)
            .apply {
                lifecycleOwner = viewLifecycleOwner
                executePendingBindings()
                viewmodel = viewModel
            }
        receiptDialogBinding = DialogTransactionResultBinding.inflate(inflater, null, false).apply {
            executePendingBindings()
        }
        progressDialog = ProgressDialog(requireContext())
            .apply {
                setCancelable(false)
                setMessage("Please wait")
            }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.selectedAction.observe(viewLifecycleOwner) {
            if (it == HISTORY_ACTION_DEFAULT)
                binding.actionButton.visibility = View.GONE
            else
                binding.actionButton.text = it
            if (it == HISTORY_ACTION_PREAUTH) {
                binding.actionButton.visibility = View.GONE
                if (viewModel.lastTransactionResponse.value!!.responseCode == "00") {
                    binding.preAuthComplete.visibility = View.VISIBLE
                    binding.preAuthRefund.visibility = View.VISIBLE
                } else {
                    val m = "This Pre Auth Transaction wasn't successful"
                    binding.message.text = m
                }
            }
        }
        binding.preAuthRefund.setOnClickListener {
            gotoAction { viewModel.preAuthRefund(requireContext()) }
        }

        binding.preAuthComplete.setOnClickListener {
            gotoAction { viewModel.doSaleCompletion(requireContext()) }
        }
        binding.actionButton.setOnClickListener {
            viewModel.performAction()
        }
        binding.details.text = viewModel.lastTransactionResponse.value!!.builder().toString()
        viewModel.done.observe(viewLifecycleOwner) {
            if (it) {
                //Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
                viewModel.reset()
            }
        }
        viewModel.showProgressDialog.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) progressDialog.show() else progressDialog.dismiss()
            }

        }
        viewModel.beginGetCardDetails.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { startCardReader ->
                if (startCardReader)
                    gotoAction { viewModel.refundTransaction(requireContext()) }
            }
        }
        viewModel.message.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { s ->
                showSnackBar(s)
            }
        }
        viewModel.smsSent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                receiptDialogBinding.progress.visibility = View.GONE
                receiptDialogBinding.sendButton.isEnabled = true
                if (it) {
                    Toast.makeText(requireContext(), "Sent Receipt", Toast.LENGTH_LONG).show()
                    alertDialog.dismiss()
                }
            }
        }
        viewModel.toastMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
        alertDialog = AlertDialog.Builder(requireContext()).setCancelable(false).apply {
            setView(receiptDialogBinding.root)
            receiptDialogBinding.apply {
                closeBtn.setOnClickListener {
                    alertDialog.dismiss()
                }
                sendButton.setOnClickListener {
                    if (receiptDialogBinding.telephone.text.toString().length != 11) {
                        Toast.makeText(
                            requireContext(),
                            "Please enter a valid phone number",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                    viewModel.sendSmS(receiptDialogBinding.telephone.text.toString())
                    progress.visibility = View.VISIBLE
                    sendButton.isEnabled = false
                }
            }
        }.create()
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        viewModel.showPrintDialog.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                alertDialog.apply {
                    receiptDialogBinding.transactionContent.text = it
                    show()
                }
                receiptDialogBinding.apply {
                    progress.visibility = View.GONE
                    sendButton.isEnabled = true
                }
            }
        }
        viewModel.shouldRefreshNibssKeys.observe(viewLifecycleOwner){event ->
            event.getContentIfNotHandled()?.let {
                if (it)
                    NetPosTerminalConfig.init(requireContext().applicationContext, configureSilently = true)
            }
        }

        viewModel.showPrinterError.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .apply {
                        setTitle("Printer Error")
                        setIcon(R.drawable.ic_warning)
                        setMessage(it)
                        setPositiveButton("Send Receipt") { d, _ ->
                            d.dismiss()
                            viewModel.showReceiptDialog()
                        }
                        setNegativeButton("Dismiss") { d, _ ->
                            d.dismiss()
                            requireActivity().onBackPressed()
                        }
                    }.show()
            }
        }
    }

    private fun gotoAction(action: () -> Unit) {
        showCardDialog(
            requireActivity(),
            viewLifecycleOwner,
            1000,
            0L
        ).observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                it.error?.let { error ->
                    Timber.e(error)
                    Toast.makeText(
                        requireContext(),
                        error.localizedMessage,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                it.cardData?.let { cardData ->
                    viewModel.setCardScheme(it.cardScheme!!)
                    viewModel.setCustomerName(it.customerName ?: "Customer")
                    viewModel.setAccountType(it.accountType!!)
                    viewModel.cardData = cardData
                    action.invoke()
                }
            }
        }
    }

    private fun showSnackBar(message: String) {
        if (message == "Transaction not approved") {
            AlertDialog.Builder(requireContext())
                .apply {
                    setTitle("Response")
                    setMessage(message)
                    show()
                }
        }
        Snackbar.make(
            requireActivity().findViewById(
                R.id.container_main
            ), message, Snackbar.LENGTH_LONG
        ).show()
    }
}