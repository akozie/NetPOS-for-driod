@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.entities.clearPinKey
import com.danbamitale.epmslib.utils.IsoAccountType
import com.danbamitale.epmslib.utils.TripleDES
import com.google.android.material.snackbar.Snackbar
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.DialogTransactionResultBinding
import com.woleapp.netpos.databinding.FragmentSalesBinding
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.TRANSACTION_TYPE
import com.woleapp.netpos.util.showCardDialog
import com.woleapp.netpos.viewmodels.SalesViewModel
import timber.log.Timber


class SalesFragment : BaseFragment() {
    companion object {
        fun newInstance(transactionType: TransactionType = TransactionType.PURCHASE): SalesFragment =
            SalesFragment().apply {
                arguments = Bundle().apply {
                    putString(TRANSACTION_TYPE, transactionType.name)
                }
            }
    }

    private val viewModel by viewModels<SalesViewModel>()
    private lateinit var transactionType: TransactionType
    private lateinit var alertDialog: AlertDialog
    private lateinit var receiptDialogBinding: DialogTransactionResultBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentSalesBinding.inflate(inflater, container, false)
        transactionType = TransactionType.valueOf(
            arguments?.getString(
                TRANSACTION_TYPE,
                TransactionType.PURCHASE.name
            )!!
        )
        receiptDialogBinding = DialogTransactionResultBinding.inflate(inflater, null, false)
            .apply { executePendingBindings() }
        binding.apply {
            viewmodel = viewModel
            lifecycleOwner = viewLifecycleOwner
            executePendingBindings()
            type = transactionType.name
        }
        viewModel.message.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { s ->
                showSnackBar(s)
            }
        }
        /*viewModel.getCardData.observe(viewLifecycleOwner){event ->
            event.getContentIfNotHandled()?.let {
                quickPay()
            }
        }*/
        viewModel.getCardData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { shouldGetCardData ->
                if (shouldGetCardData)
                    showCardDialog(
                        requireActivity(),
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
                                viewModel.makePayment(requireContext(), transactionType)
                            }
                        }
                    }
            }
        }
        alertDialog = AlertDialog.Builder(requireContext()).setCancelable(false).apply {
            setView(receiptDialogBinding.root)
            receiptDialogBinding.apply {
                sendButton.setOnClickListener {
                    progress.visibility = View.VISIBLE
                    sendButton.isEnabled = false
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(requireContext(),"Sent Receipt", Toast.LENGTH_LONG).show()
                        alertDialog.dismiss()
                    }, 2000)
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
        binding.process.setOnClickListener {
            viewModel.validateField()
        }
        return binding.root
    }

    /*private fun quickPay() {
        viewModel.setAccountType(IsoAccountType.SAVINGS)
        viewModel.cardData = CardData(
            track2Data = "5199110748591994D2012221000013772F",
            nibssIccSubset = "9F26087BCB3647E84652A29F2701809F10120110A040002A0400000000000000000000FF9F370451271F779F360201E3950500802480009A032009179C01009F02060000000010005F2A020566820239009F1A0205669F34034203009F3303E0F9C89F3501229F1E0831323334353637388407A00000000410109F090200009F03060000000000005F340100",
            panSequenceNumber = "000",
            posEntryMode = "051"
        ).apply {
            pinBlock = TripleDES.encrypt(
                "0425A8EF8B7A6E66",
                NetPosTerminalConfig.getKeyHolder()!!.clearPinKey
            )
        }
        viewModel.makePayment(requireContext(), transactionType)
    }*/

    private fun quickPay() {
        viewModel.setAccountType(IsoAccountType.SAVINGS)
        viewModel.setCardScheme("Master Card")
        viewModel.setCustomerName("SUBAIR/BABATUNDE")
        viewModel.cardData = CardData(
            track2Data = "5399834599607066D22032210014182625",
            nibssIccSubset = "9F2608F564EF96AC6AFE8F9F2701809F10120110A04003220000000000000000000000FF9F3704BAD5E42A9F3602030A950500000480009A032012039C01009F02060000000010005F2A020566820239009F1A0205669F34034203009F3303E068C89F3501229F1E0842313739314531588407A00000000410109F090200029F03060000000000005F340101",
            panSequenceNumber = "001",
            posEntryMode = "051"
        ).apply {
            pinBlock = TripleDES.encrypt(
                "0420BDCBA669F8F9",
                NetPosTerminalConfig.getKeyHolder()!!.clearPinKey
            )
        }
        viewModel.makePayment(requireContext(), transactionType)
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