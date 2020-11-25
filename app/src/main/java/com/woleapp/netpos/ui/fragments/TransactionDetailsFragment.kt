@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.FragmentTransactionDetailsBinding
import com.woleapp.netpos.util.HISTORY_ACTION_DEFAULT
import com.woleapp.netpos.util.showCardDialog
import com.woleapp.netpos.viewmodels.TransactionsViewModel
import timber.log.Timber

class TransactionDetailsFragment : BaseFragment() {
    private val viewModel by activityViewModels<TransactionsViewModel>()
    private lateinit var binding: FragmentTransactionDetailsBinding
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.selectedAction.observe(viewLifecycleOwner) {
            if (it == HISTORY_ACTION_DEFAULT)
                binding.actionButton.visibility = View.GONE
            else
                binding.actionButton.text = it
        }
        binding.actionButton.setOnClickListener {
            viewModel.performAction()
        }
        viewModel.done.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
                viewModel.reset()
            }
        }
        viewModel.beginGetCardDetails.observe(viewLifecycleOwner) {event ->
            event.getContentIfNotHandled()?.let { startCardReader ->
                if (startCardReader)
                    showCardDialog(requireActivity(), 1000, 0L).observe(viewLifecycleOwner) { event ->
                        event.getContentIfNotHandled()?.let {
                            it.error?.let { error ->
                                Timber.e(error)
                                Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_SHORT)
                                    .show()
                            }
                            it.cardData?.let { cardData ->
                                viewModel.setCardScheme(it.cardScheme!!)
                                viewModel.setCustomerName(it.customerName!!)
                                viewModel.setAccountType(it.accountType!!)
                                viewModel.cardData = cardData
                                viewModel.refundTransaction(requireContext())
                            }
                        }
                    }
            }
        }
        viewModel.message.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { s ->
                showSnackBar(s)
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