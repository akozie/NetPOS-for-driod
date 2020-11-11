@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.utils.IsoAccountType
import com.google.android.material.snackbar.Snackbar
import com.netpluspay.kozenlib.emv.CardReaderEvent
import com.netpluspay.kozenlib.emv.CardReaderService
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.DialogSelectAccountTypeBinding
import com.woleapp.netpos.databinding.FragmentTransactionDetailsBinding
import com.woleapp.netpos.util.HISTORY_ACTION_DEFAULT
import com.woleapp.netpos.util.disposeWith
import com.woleapp.netpos.viewmodels.TransactionsViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class TransactionDetailsFragment : BaseFragment() {
    private val viewModel by activityViewModels<TransactionsViewModel>()
    private lateinit var binding: FragmentTransactionDetailsBinding
    private lateinit var dialogSelectAccountTypeBinding: DialogSelectAccountTypeBinding
    private val compositeDisposable = CompositeDisposable()
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
        viewModel.message.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { s ->
                showSnackBar(s)
            }
        }
    }

    private fun showCardDialog() {
        var hasCardBeenRead = false
        val dialog = ProgressDialog(requireContext())
            .apply {
                setMessage("Waiting for card")
                setCancelable(false)
            }
        val c = CardReaderService(requireActivity()).initiateICCCardPayment(
            1000,
            0L,
            null
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                when (it) {
                    is CardReaderEvent.CardRead -> {
                        hasCardBeenRead = true
                        val cardResult = it.getData()
                        val card = CardData(
                            track2Data = cardResult.track2Data!!,
                            nibssIccSubset = cardResult.nibssIccSubset,
                            panSequenceNumber = cardResult.applicationPANSequenceNumber!!,
                            posEntryMode = "051"
                        ).apply {
                            pinBlock = cardResult.encryptedPinBlock
                        }
                        viewModel.setCustomerName(cardResult.cardHolderName!!)
                        viewModel.setCardScheme(cardResult.cardScheme)
                        viewModel.cardData = card
//                        val cardReaderMqttEvent = CardReaderMqttEvent(
//                            cardExpiry = cardResult.expirationDate,
//                            cardHolder = cardResult.cardHolderName,
//                            maskedPan = StringUtils.overlay(
//                                cardResult.applicationPANSequenceNumber,
//                                "xxxxxx",
//                                6,
//                                12
//                            )
//                        )
//                        viewModel.sendCardEvent("SUCCESS", "00", cardReaderMqttEvent)
                    }
                    is CardReaderEvent.CardDetected -> {
                        hasCardBeenRead = true
                        dialog.setMessage("Reading Card Please Wait")
                        Timber.e("Card Detected")
                    }
                }
            }, {
                it?.let {
//                    val cardReaderMqttEvent = CardReaderMqttEvent(readerError = it.localizedMessage)
//                    viewModel.sendCardEvent("ERROR", "99", cardReaderMqttEvent)
                    dialog.dismiss()
                    Timber.e("error: ${it.localizedMessage}")
                    Toast.makeText(
                        requireContext(),
                        "Error: ${it.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }, {
                dialog.dismiss()
                Toast.makeText(requireContext(), "complete", Toast.LENGTH_LONG).show()
                Timber.e("complete")
                showSelectAccountTypeDialog()
            })
        c.disposeWith(compositeDisposable)
        Handler().postDelayed({
            if (!hasCardBeenRead) {
                Toast.makeText(requireContext(), "Timed out while waiting for card", Toast.LENGTH_LONG).show()
                c.dispose()
                dialog.dismiss()
            }
        }, 45000)
        dialog.show()
    }

    private fun showSelectAccountTypeDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .apply {
                dialogSelectAccountTypeBinding =
                    DialogSelectAccountTypeBinding.inflate(LayoutInflater.from(requireContext()), null, false).apply {
                        lifecycleOwner = viewLifecycleOwner
                        executePendingBindings()
                    }
                setView(dialogSelectAccountTypeBinding.root)
                setCancelable(false)
            }.create()
        dialogSelectAccountTypeBinding.accountTypes.setOnCheckedChangeListener { _, checkedId ->
            val accountType = when (checkedId) {
                R.id.savings_account -> IsoAccountType.SAVINGS
                R.id.current_account -> IsoAccountType.CURRENT
                R.id.credit_account -> IsoAccountType.CREDIT
                R.id.bonus_account -> IsoAccountType.BONUS_ACCOUNT
                R.id.investment_account -> IsoAccountType.INVESTMENT_ACCOUNT
                R.id.universal_account -> IsoAccountType.UNIVERSAL_ACCOUNT
                else -> IsoAccountType.DEFAULT_UNSPECIFIED
            }
            dialog.dismiss()
            Timber.e("$checkedId")
            if (accountType != IsoAccountType.DEFAULT_UNSPECIFIED) {
                viewModel.setAccountType(accountType)
                viewModel.refundTransaction(requireContext())
            }
        }
        dialogSelectAccountTypeBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(requireContext(), "Operation was cancelled", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
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