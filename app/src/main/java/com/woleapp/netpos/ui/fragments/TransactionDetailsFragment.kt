@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.fragment.app.activityViewModels
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.clearPinKey
import com.danbamitale.epmslib.utils.TripleDES
import com.netplus.sunyardlib.CardReaderEvent
import com.netplus.sunyardlib.CardReaderService
import com.netplus.sunyardlib.GetPin
import com.socsi.aidl.pinservice.OperationPinListener
import com.socsi.smartposapi.ped.KeyBoardConstant
import com.socsi.smartposapi.ped.Ped
import com.socsi.utils.HexDump
import com.socsi.utils.Log
import com.sunyard.i80.util.Util
import com.woleapp.netpos.databinding.FragmentTransactionDetailsBinding
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.HISTORY_ACTION_DEFAULT
import com.woleapp.netpos.util.xorHex
import com.woleapp.netpos.viewmodels.TransactionsViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class TransactionDetailsFragment : BaseFragment() {
    private val viewModel by activityViewModels<TransactionsViewModel>()
    private lateinit var binding: FragmentTransactionDetailsBinding
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
    }

    private fun showCardDialog() {
        val dialog = ProgressDialog(requireContext())
            .apply {
                setMessage("Waiting for card")
                setCancelable(false)
            }
        val c = CardReaderService.initiateICCCardPayment(
            context,
            1000,
            0L,
            NetPosTerminalConfig.getKeyHolder()?.clearPinKey
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                when (it) {
                    is CardReaderEvent.CardRead -> {
                        val cardResult = it.data
                        Timber.e(cardResult.toString())
                        Timber.e("pinblock: ${cardResult.pinBlock}")
                        Timber.e("nibss pin: ${cardResult.encryptedPinBlock}")
                        Timber.e("nibss subset: ${cardResult.nibssIccSubset}")
                        Timber.e("Card Holder name: ${HexDump.hexStr2Str(cardResult.cardHolderName)}")
                        val card = CardData(
                            track2Data = cardResult.track2Data,
                            nibssIccSubset = cardResult.nibssIccSubset,
                            panSequenceNumber = cardResult.applicationPANSequenceNumber,
                            posEntryMode = "051"
                        ).apply {
                            pinBlock = cardResult.encryptedPinBlock
                        }
                        viewModel.cardData = card
                    }
                    is CardReaderEvent.CardDetected -> {
                        dialog.setMessage("Reading Card Please Wait")
                        Timber.e("Card Detected")
                    }
                }
            }, {
                it?.let {
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
                viewModel.refundTransaction(requireContext())
            })
        dialog.show()
        compositeDisposable.add(c)
    }


    @Suppress("DEPRECATION")
    private fun showPinpad(
        pan: String,
        cardData: CardData
    ) {
        Timber.e("Pan passed to pin Handler: $pan")
        val param = Bundle()
        param.putBoolean("isOnline", false)
        param.putString("pan", pan)
        param.putString("promptString", "Please input the pin")
        param.putIntArray("pinLimit", intArrayOf(4, 5, 6, 7, 8, 9, 10))
        param.putByte("keysType", Ped.KEYS_TYPE_MK_SK)
        param.putByte("pinAlgMode", Ped.ALGORITHMTYPE_USE_PAN_SUPPLY_F)
        param.putByteArray("random", Util.StringToBytes("AACC9675BBA5EF44"))
        param.putInt(KeyBoardConstant.BUNDLE_KEY_DESTYPE, 1)
        //param.putInt(KeyBoardConstant.BUNDLE_KEY_KEYSTYPE, 1)
        param.putInt("timeout", 60)
        Ped.getInstance()
            .startPinInput(requireContext(), 1, param, object : OperationPinListener {
                override fun onInput(len: Int, key: Int) {
                    showToastOnUiThread("Len: $len")
                    Log.e("ContentValues", "onInput  len:$len  key:$key")
                }

                override fun onError(errorCode: Int) {
                    showToastOnUiThread("Error: $errorCode")
                    Timber.e("onError:::: errorCode:$errorCode")
                }

                override fun onConfirm(@Nullable data: ByteArray, @Nullable isNonePin: Boolean) {
                    showToastOnUiThread("Confirm: ${Util.BytesToString(data)}")
                    Timber.e("ContentValues ${Util.BytesToString(data)}")
//                    val encryptedPin = Ped.getInstance().encryptPIN(1, 1, 1, data, "")
//                    Timber.e(encryptedPin)
                    Timber.e("Byte Array ${data.toString()}")
                    val pinFromPad = Util.BytesToString(data)
//                    val pinData: String = Util.BytesToString(
//                        Util.concat(
//                            data,
//                            Dukpt.getInstance().currentukptKsn
//                        )
//                    )
                    Timber.e(
                        pinFromPad
                    )
//                    val encryptedPin = Ped.getInstance().encryptPIN(1,1,1, data, "")
//                    Timber.e("encryptedPin: $encryptedPin")
                    requireActivity().runOnUiThread {
                        cardData.apply {
                            val pin = "04${pinFromPad}FFFFFFFFFF"
                            val cardNum = "0000${pan.substring(3, 15)}"
                            val hexed = xorHex(pin, cardNum)
                            Timber.e("hexed: $hexed")
                            pinBlock = TripleDES.encrypt(
                                hexed,
                                NetPosTerminalConfig.getKeyHolder()?.clearPinKey
                            )
                        }
                        Timber.e(cardData.toString())
                        viewModel.cardData = cardData
                        viewModel.refundTransaction(requireContext())
                    }
                }

                override fun onCancel() {
                    showToastOnUiThread("canceled")
                    Timber.e("onCancel")
                }
            })
    }

    private fun showToastOnUiThread(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
}