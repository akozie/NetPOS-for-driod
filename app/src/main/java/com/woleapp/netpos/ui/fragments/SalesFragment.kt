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
import androidx.annotation.Nullable
import androidx.fragment.app.viewModels
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.KeyHolder
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.entities.clearPinKey
import com.danbamitale.epmslib.utils.IsoAccountType
import com.danbamitale.epmslib.utils.TripleDES
import com.google.android.material.snackbar.Snackbar
import com.netplus.sunyardlib.CardReaderEvent
import com.netplus.sunyardlib.CardReaderService
import com.netplus.sunyardlib.GetPin
import com.socsi.aidl.pinservice.OperationPinListener
import com.socsi.smartposapi.ped.KeyBoardConstant
import com.socsi.smartposapi.ped.Ped
import com.socsi.smartposapi.ped.Ped.KEYS_TYPE_MK_SK
import com.socsi.utils.HexDump
import com.socsi.utils.Log
import com.sunyard.i80.util.Util
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.DialogSelectAccountTypeBinding
import com.woleapp.netpos.databinding.FragmentSalesBinding
import com.woleapp.netpos.model.CardReaderMqttEvent
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.TRANSACTION_TYPE
import com.woleapp.netpos.util.disposeWith
import com.woleapp.netpos.util.xorHex
import com.woleapp.netpos.viewmodels.SalesViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang.StringUtils
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
                        hasCardBeenRead = true
                        val cardResult = it.data
                        Timber.e(cardResult.toString())
                        Timber.e("nibss pin: ${cardResult.encryptedPinBlock}")
                        Timber.e("nibss subset: ${cardResult.nibssIccSubset}")
                        Timber.e("Card Holder name: ${HexDump.hexStr2Str(cardResult.cardHolderName)}")
                        viewModel.setCustomerName(
                            HexDump.hexStr2Str(cardResult.cardHolderName).replace("/", " ")
                        )
                        val card = CardData(
                            track2Data = cardResult.track2Data,
                            nibssIccSubset = cardResult.nibssIccSubset,
                            panSequenceNumber = cardResult.applicationPANSequenceNumber,
                            posEntryMode = "051"
                        ).apply {
                            pinBlock = cardResult.encryptedPinBlock
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

//    @Suppress("DEPRECATION")
//    private fun showPinpad(
//        pan: String,
//        cardData: CardData
//    ) {
//        Timber.e("Pan passed to pin Handler: $pan")
//        val param = CardUtil.buildParam(pan)
//        val nibssKeyholderKey = NetPosTerminalConfig.getKeyHolder()?.clearPinKey!!
//        Ped.getInstance().loadMKey(
//            0xff.toByte(), 1, mainKeyDefault, 1,
//            Ped.KEY_TYPE_UNENCTYPTED_KEY.toInt(), null, false
//        )
//        val s = Ped.getInstance().loadWorkKeyByIdx(1, Ped.WORK_KEY_TYPE_PIN_KEY, 1,
//            nibssKeyholderKey, null)
//        Timber.e("loaded work key? $s")
//        //val ped:Ped = Ped.getInstance()
//        //ped.loadMKey(Ped.KEY_TYPE_DES_KEY, nibssKeyholderKey, true)
//        Ped.getInstance()
//            .startPinInput(requireContext(), 1, param, object : OperationPinListener {
//                override fun onInput(len: Int, key: Int) {
//                    showToastOnUiThread("Len: $len")
//                    Log.e("ContentValues", "onInput  len:$len  key:$key")
//                }
//
//                override fun onError(errorCode: Int) {
//                    showToastOnUiThread("Error: $errorCode")
//                    Timber.e("onError:::: errorCode:$errorCode")
//                }
//
//                override fun onConfirm(@Nullable data: ByteArray, @Nullable isNonePin: Boolean) {
//                    showToastOnUiThread("Confirm: ${Util.BytesToString(data)}")
//                    Timber.e("ContentValues ${Util.BytesToString(data)}")
////                    val encryptedPin = Ped.getInstance().encryptPIN(1, 1, 1, data, "")
////                    Timber.e(encryptedPin)
//                    Timber.e("Byte Array ${data.toString()}")
//                    val pinFromPad = Util.BytesToString(data)
//                    val pinData: String = Util.BytesToString(
//                        Util.concat(
//                            data,
//                            Dukpt.getInstance().currentukptKsn
//                        )
//                    )
//                    Timber.e(
//                        pinFromPad
//                    )
////                    val encryptedPin = Ped.getInstance().encryptPIN(1,1,1, data, "")
////                    Timber.e("encryptedPin: $encryptedPin")
//                    viewModel.setCardPin(pinFromPad)
//                    requireActivity().runOnUiThread {
//                        cardData.apply {
//                            val pin = "04${pinFromPad}FFFFFFFFFF"
//                            val cardNum = "0000${pan.substring(3, 15)}"
//                            //val hexed = xorHex(pin, cardNum)
//                            //Timber.e("hexed: $hexed")
//                            pinBlock = pinFromPad
////                            pinBlock = TripleDES.encrypt(
////                                hexed,
////                                NetPosTerminalConfig.getKeyHolder()?.clearPinKey
////                            )
//                        }
//                        Timber.e(cardData.toString())
//                        viewModel.cardData = cardData
//                        viewModel.makePayment(requireContext(), transactionType)
//                    }
//                }
//
//                override fun onCancel() {
//                    showToastOnUiThread("canceled")
//                    Timber.e("onCancel")
//                }
//            })
//    }

    @Suppress("DEPRECATION")
    private fun showPinpad(
        pan: String,
        cardData: CardData
    ) {
        Timber.e("Pan passed to pin Handler: $pan")
        val param = Bundle()
        val clearPinKey = NetPosTerminalConfig.getKeyHolder()?.clearPinKey!!
        param.putBoolean("isOnline", false)
        param.putString("pan", pan)
        param.putString("promptString", "Please input the pin")
        param.putIntArray("pinLimit", intArrayOf(4, 5, 6, 7, 8, 9, 10))
        param.putByte("keysType", KEYS_TYPE_MK_SK)
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
                    Timber.e("Byte Array ${data.toString()}")
                    val pinFromPad = Util.BytesToString(data)
                    requireActivity().runOnUiThread {
                        cardData.apply {
                            val pin = "04${pinFromPad}FFFFFFFFFF"
                            val cardNum = "0000${pan.substring(3, 15)}"
                            val hexed = xorHex(pin, cardNum)
                            Timber.e("hexed: $hexed")
                            pinBlock = TripleDES.encrypt(
                                hexed,
                                clearPinKey
                            )
                        }
                        Timber.e(cardData.toString())
                        viewModel.cardData = cardData
                        viewModel.makePayment(requireContext(), transactionType)
                    }
                }

                override fun onCancel() {
                    showToastOnUiThread("canceled")
                    Timber.e("onCancel")
                }
            })
    }
    /*@Suppress("DEPRECATION")
    private fun showPinpad(
        pan: String,
        getPinHandler: com.socsi.smartposapi.emv2.EmvL2.GetPinHandler? = null,
        proceed: Boolean = false
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
                    getPinHandler?.onGetPin(1, data)
                    showToastOnUiThread("Confirm: ${Util.BytesToString(data)}")
                    Timber.e("ContentValues ${Util.BytesToString(data)}")
//                    val encryptedPin = Ped.getInstance().encryptPIN(1, 1, 1, data, "")
//                    Timber.e(encryptedPin)
                    Timber.e("Byte Array ${data.toString()}")
                    val pin = Util.BytesToString(data)
                    val pinData: String = Util.BytesToString(
                        Util.concat(
                            data,
                            Dukpt.getInstance().currentukptKsn
                        )
                    )
                    Timber.e(
                        pin
                    )
//                    val encryptedPin = Ped.getInstance().encryptPIN(1,1,1, data, "")
//                    Timber.e("encryptedPin: $encryptedPin")
                    viewModel.setCardPin(pin)
                    if (proceed)
                        requireActivity().runOnUiThread {
                            viewModel.makePayment(requireContext(), transactionType)
                        }
                }

                override fun onCancel() {
                    showToastOnUiThread("canceled")
                    Timber.e("onCancel")
                }
            })
    }*/

    private fun showToastOnUiThread(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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