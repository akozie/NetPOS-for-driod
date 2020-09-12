@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
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
import com.netplus.sunyardlib.CardReaderService
import com.netplus.sunyardlib.GetPin
import com.socsi.aidl.pinservice.OperationPinListener
import com.socsi.smartposapi.ped.KeyBoardConstant
import com.socsi.smartposapi.ped.Ped
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
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Input Card")
            .setMessage("Please Insert Your card").show()
        val getPin =
            GetPin { pan, getPinHandler ->
                showToastOnUiThread("GetPin Here")
                //showPinpad(Util.BytesToString(pan), getPinHandler)
                getPinHandler!!.onGetPin(1, pan)
            }
        val c = CardReaderService.initiateICCCardPayment(context, 1000, 0L, getPin)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { cardData, throwable ->
                cardData?.let { cardReadResult ->
                    try {
                        val card = CardData(
                            track2Data = cardReadResult.track2Data,
                            nibssIccSubset = cardReadResult.nibssIccSubset,
                            panSequenceNumber = cardReadResult.applicationPANSequenceNumber,
                            posEntryMode = "051"
                        )
                        showPinpad(cardData.applicationPrimaryAccountNumber, card)
                        //viewModel.cardData = card
                        //Timber.e(card.toString())
//                        if (viewModel.pin == null)
//                            showPinpad(
//                                cardReadResult.applicationPrimaryAccountNumber,
//                                proceed = true
//                            )
//                        else
//                            viewModel.makePayment(requireContext(), transactionType)
                        //viewModel.makePayment(requireContext(), transactionType)
                        Timber.e("Card $card")
                        //viewModel.makePayment(requireContext(), transactionType)
                    } catch (e: Exception) {
                        Timber.e("error: ${e.localizedMessage}")
                        Toast.makeText(
                            requireContext(),
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    //Timber.e("icc data: ${cardReadResult.nibssIccSubset}")
                    //Timber.e(cardReadResult.applicationPANSequenceNumber)
                    //Timber.e(cardReadResult.applicationPrimaryAccountNumber)
                    //Timber.e(cardReadResult.toString())
                    if (dialog.isShowing)
                        dialog.dismiss()

                    Toast.makeText(context, "Done Reading Card", Toast.LENGTH_SHORT).show()
                    //Timber.e(it.toString())
                }
                throwable?.let {
                    Toast.makeText(context, "Error ${it.localizedMessage}", Toast.LENGTH_SHORT)
                        .show()
                    Timber.e(it)
                    if (dialog.isShowing)
                        dialog.dismiss()
                }
            }
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