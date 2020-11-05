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
import androidx.fragment.app.viewModels
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.entities.clearPinKey
import com.danbamitale.epmslib.utils.IsoAccountType
import com.danbamitale.epmslib.utils.TripleDES
import com.google.android.material.snackbar.Snackbar
import com.netpluspay.kozenlib.emv.CardReaderEvent
import com.netpluspay.kozenlib.emv.CardReaderService
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.DialogSelectAccountTypeBinding
import com.woleapp.netpos.databinding.FragmentSalesBinding
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.TRANSACTION_TYPE
import com.woleapp.netpos.util.disposeWith
import com.woleapp.netpos.viewmodels.SalesViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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

    private lateinit var dialogSelectAccountTypeBinding: DialogSelectAccountTypeBinding
    private val viewModel by viewModels<SalesViewModel>()
    private lateinit var transactionType: TransactionType
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentSalesBinding.inflate(inflater, container, false)
        dialogSelectAccountTypeBinding =
            DialogSelectAccountTypeBinding.inflate(inflater, container, false).apply {
                lifecycleOwner = viewLifecycleOwner
                executePendingBindings()
            }
        transactionType = TransactionType.valueOf(
            arguments?.getString(
                TRANSACTION_TYPE,
                TransactionType.PURCHASE.name
            )!!
        )
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

        binding.process.setOnClickListener { showCardDialog() }
        return binding.root
    }

    private fun quickPay() {
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
            NetPosTerminalConfig.getKeyHolder()?.clearPinKey
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                when (it) {
                    is CardReaderEvent.CardRead -> {
                        hasCardBeenRead = true
                        val cardResult = it.getData()
                        Timber.e(cardResult.toString())
                        Timber.e("nibss pin: ${cardResult.encryptedPinBlock}")
                        Timber.e("nibss subset: ${cardResult.nibssIccSubset}")
                        Timber.e("Card Holder name: ${cardResult.cardHolderName}")
                        Timber.e("pinblock: ${cardResult.encryptedPinBlock}")
                        viewModel.setCustomerName(
                            cardResult.cardHolderName!!
                        )
                        val card = CardData(
                            track2Data = cardResult.track2Data!!,
                            nibssIccSubset = cardResult.nibssIccSubset,
                            panSequenceNumber = cardResult.applicationPANSequenceNumber!!,
                            posEntryMode = "051"
                        ).apply {
                            pinBlock = TripleDES.encrypt("0425396E7EBEBBBD", NetPosTerminalConfig.getKeyHolder()?.clearPinKey)
                        }
                        //Timber.e(card.toString())
                        //Timber.e("pinblock: ${cardResult.encryptedPinBlock}")
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
        val handler = Handler().postDelayed({
            if (!hasCardBeenRead) {
                Toast.makeText(requireContext(), "Timed out while waiting for card", Toast.LENGTH_LONG).show()
                c.dispose()
                dialog.dismiss()
            }
        }, 45000)
        dialog.show()
        //compositeDisposable.add(c)
    }

    private fun showSelectAccountTypeDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .apply {
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
            viewModel.setAccountType(accountType)
            dialog.dismiss()
            viewModel.makePayment(requireContext(), transactionType)
            Timber.e("$checkedId")
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