@file:Suppress("DEPRECATION")

package com.woleapp.netpos.util

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.utils.IsoAccountType
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.emv.CardReadResult
import com.netpluspay.netpossdk.emv.CardReaderEvent
import com.netpluspay.netpossdk.emv.CardReaderService
import com.pos.sdk.emvcore.POIEmvCoreManager.DEV_ICC
import com.pos.sdk.emvcore.POIEmvCoreManager.DEV_PICC
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.DialogSelectAccountTypeBinding
import com.woleapp.netpos.model.CardReaderMqttEvent
import com.woleapp.netpos.model.MqttEvent
import com.woleapp.netpos.model.MqttEvents
import com.woleapp.netpos.model.MqttTopics
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang.StringUtils
import timber.log.Timber

data class ICCCardHelper(
    val cardReadResult: CardReadResult? = null,
    val customerName: String? = null,
    val cardScheme: String? = null,
    var accountType: IsoAccountType? = null,
    val cardData: CardData? = null,
    val error: Throwable? = null
)


fun showCardDialog(
    context: Activity,
    lifecycleOwner: LifecycleOwner,
    amount: Long,
    cashBackAmount: Long,
    compositeDisposable: CompositeDisposable? = null
): LiveData<Event<ICCCardHelper>> {
    var configurationFinished = false
    val liveData: MutableLiveData<Event<ICCCardHelper>> = MutableLiveData()
    if (NetPosTerminalConfig.liveData.hasActiveObservers())
        NetPosTerminalConfig.liveData.removeObservers(lifecycleOwner)
    val progressDialog = ProgressDialog(context)
    progressDialog.setMessage("connecting, please wait...")
    var observer: Observer<Event<Int>>? = null
    observer = Observer<Event<Int>> {
        it.getContentIfNotHandled()?.let { int ->
            Timber.e("picked up: $int")
            when (int) {
                0 -> progressDialog.show()
                1 -> {
                    configurationFinished = true
                    if (progressDialog.isShowing)
                        progressDialog.dismiss()
                    getCardLiveData(context, amount, cashBackAmount, liveData, compositeDisposable)
                    NetPosTerminalConfig.liveData.removeObserver(observer!!)
                }
                -1 -> {
                    configurationFinished = true
                    if (progressDialog.isShowing)
                        progressDialog.dismiss()
                    if (NetPosTerminalConfig.getTerminalId().isEmpty()) {
                        Toast.makeText(context, "No TID found on account", Toast.LENGTH_SHORT)
                            .show()
                    } else
                        Toast.makeText(context, "Connection Failed", Toast.LENGTH_SHORT).show()
                }
                else -> {
                }
            }
        }
    }
    NetPosTerminalConfig.liveData.observe(lifecycleOwner, observer)
    when {
        NetPosTerminalConfig.configurationStatus != 1 && NetPosTerminalConfig.isConfigurationInProcess.not() -> NetPosTerminalConfig.init(
            context.applicationContext
        )
        NetPosTerminalConfig.isConfigurationInProcess -> if (configurationFinished.not()) progressDialog.show()
        NetPosTerminalConfig.configurationStatus == 1 -> getCardLiveData(
            context,
            amount,
            cashBackAmount,
            liveData
        )
    }
    return liveData
}

fun getCardLiveData(
    context: Activity,
    amount: Long,
    cashBackAmount: Long,
    liveData: MutableLiveData<Event<ICCCardHelper>>,
    compositeDisposable: CompositeDisposable? = null
) {
    val dialog = ProgressDialog(context)
        .apply {
            setMessage("Waiting for card")
            //setCancelable(false)
        }
    var iccCardHelper: ICCCardHelper? = null
    val cardService = CardReaderService(context, listOf(DEV_ICC, DEV_PICC))
    val c = cardService.initiateICCCardPayment(
        amount,
        cashBackAmount
    )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
            when (it) {
                is CardReaderEvent.CardRead -> {
                    val cardResult: CardReadResult = it.data

//                    Timber.e(cardResult.iccDataString)
//                    Timber.e(cardResult.nibssIccSubset)
                    val card = CardData(
                        track2Data = cardResult.track2Data!!,
                        nibssIccSubset = cardResult.nibssIccSubset,
                        panSequenceNumber = cardResult.applicationPANSequenceNumber!!,
                        posEntryMode = "051"
                    )
                    cardResult.cardScheme
                    if (cardResult.encryptedPinBlock.isNullOrEmpty().not()) {
                        card.apply {
                            pinBlock = cardResult.encryptedPinBlock
                        }
                    }
                    Timber.e("icc string")
                    Timber.e(cardResult.iccDataString)
                    Timber.e(card.toString())
                    iccCardHelper = ICCCardHelper(
                        cardReadResult = cardResult,
                        customerName = cardResult.cardHolderName,
                        cardScheme = cardResult.cardScheme,
                        cardData = card
                    )
                    val cardReaderMqttEvent = CardReaderMqttEvent(
                        cardExpiry = cardResult.expirationDate,
                        cardHolder = cardResult.cardHolderName,
                        maskedPan = StringUtils.overlay(
                            cardResult.applicationPANSequenceNumber,
                            "xxxxxx",
                            6,
                            12
                        )
                    )
                    sendCardEvent("SUCCESS", "00", cardReaderMqttEvent)
                }
                is CardReaderEvent.CardDetected -> {
                    val mode = when (it.mode) {
                        DEV_ICC -> "EMV"
                        DEV_PICC -> "EMV Contactless"
                        else -> "MAGNETIC STRIPE"
                    }
                    dialog.setMessage("Reading Card with $mode Please Wait")
                    Timber.e("Card Detected")
                }
                else -> {

                }
            }
        }, {
            it?.let {
                dialog.dismiss()
                sendCardEvent("ERROR", "99", CardReaderMqttEvent(readerError = it.localizedMessage))
                Timber.e("error: ${it.localizedMessage}")
                liveData.value = Event(ICCCardHelper(error = it))
            }

        }, {
            dialog.dismiss()
            showSelectAccountTypeDialog(context, iccCardHelper!!, liveData)
        })

    dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Stop") { d, _ ->
        cardService.transEnd(message = "Stopped")
        d.dismiss()
    }
    dialog.show()
    compositeDisposable?.add(c)
}

fun sendCardEvent(s: String, s1: String, cardReaderMqttEvent: CardReaderMqttEvent) {
    val user = Singletons.getCurrentlyLoggedInUser()
    val event = MqttEvent(
        user!!.netplus_id!!,
        user.business_name!!,
        NetPosTerminalConfig.getTerminalId(),
        NetPosSdk.getDeviceSerial()
    )
    event.apply {
        this.event = MqttEvents.CARD_READER_EVENTS.event
        data = cardReaderMqttEvent
        this.status = s
        timestamp = System.currentTimeMillis()
        this.code = s1
    }
    MqttHelper.sendPayload(MqttTopics.CARD_READER_EVENTS, event)
}

private fun showSelectAccountTypeDialog(
    context: Activity,
    iccCardHelper: ICCCardHelper,
    liveData: MutableLiveData<Event<ICCCardHelper>>
) {
    var dialogSelectAccountTypeBinding: DialogSelectAccountTypeBinding
    val dialog = AlertDialog.Builder(context)
        .apply {
            dialogSelectAccountTypeBinding =
                DialogSelectAccountTypeBinding.inflate(LayoutInflater.from(context), null, false)
                    .apply {
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
            iccCardHelper.apply {
                this.accountType = accountType
            }
            liveData.value = Event(iccCardHelper)
        }
    }
    dialogSelectAccountTypeBinding.cancelButton.setOnClickListener {
        dialog.dismiss()
        liveData.value = Event(ICCCardHelper(error = Throwable("Operation was canceled")))
    }
    dialog.show()
}
