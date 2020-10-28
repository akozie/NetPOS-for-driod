@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.woleapp.netpos.databinding.FragmentTransactionDetailsBinding
import com.woleapp.netpos.util.HISTORY_ACTION_DEFAULT
import com.woleapp.netpos.viewmodels.TransactionsViewModel

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
                showFragment(DashboardFragment(), DashboardFragment::class.simpleName)
                viewModel.reset()
            }
        }
        viewModel.beginGetCardDetails.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { startCardReader ->
                if (startCardReader)
                    showCardDialog()
            }
        }
    }

    private fun showCardDialog() {

    }
}