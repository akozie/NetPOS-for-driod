@file:Suppress("DEPRECATION")

package com.woleapp.netpos.util

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.utils.IsoAccountType
import com.mastercard.terminalsdk.listeners.PaymentDataProvider
import com.mastercard.terminalsdk.utility.ByteArrayWrapper
import com.woleapp.netpos.R
import com.woleapp.netpos.app.NetPosApp
import com.woleapp.netpos.databinding.DialogSelectAccountTypeBinding
import com.woleapp.netpos.model.CardReaderMqttEvent
import com.woleapp.netpos.model.MqttEvent
import com.woleapp.netpos.model.MqttEvents
import com.woleapp.netpos.model.MqttTopics
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.taponphone.listener.TransactionListener
import com.woleapp.netpos.ui.dialog.PasswordDialog
import io.reactivex.disposables.CompositeDisposable
import org.apache.commons.lang.StringUtils
import timber.log.Timber


data class ICCCardHelper(
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
                    //getFakeCardLiveData(liveData)
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
        NetPosTerminalConfig.configurationStatus == 1 ->
            getCardLiveData(
                context,
                amount,
                cashBackAmount,
                liveData
            )
        //getFakeCardLiveData(liveData)
    }
    return liveData
}

private fun cardData(): CardData {
    return CardData(
        "5199110749432974D2408221005006366F",
        "9F26089E6C9D02727937F19F2701809F10120110A74003020000000000000000000000FF9F3704A70BE8C29F3602002B950500000080009A032109179C01009F02060000000002005F2A020566820239009F1A0205669F34034403029F3303E0F8C89F3501229F1E0842313739314531588407A00000000410109F090200029F03060000000000005F340100",
        "000",
        "051"
    )
}

fun getFakeCardLiveData_(liveData: MutableLiveData<Event<ICCCardHelper>>) {
    val iccHelper = ICCCardHelper(
        "Oluwatayo/Adegboye",
        "Mastercard",
        IsoAccountType.DEFAULT_UNSPECIFIED,
        cardData()
    )
    liveData.postValue(Event(iccHelper))
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


    dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Stop") { d, _ ->
        Toast.makeText(context, "transaction aborted", Toast.LENGTH_SHORT).show()
        NetPosApp.INSTANCE.transactionsApi.abortTransaction()
        d.dismiss()
    }
    NetPosApp.INSTANCE.outcomeObserver.resetObserver(object : TransactionListener {
        override fun onTransactionSuccessful() {

        }

        override fun onOnlineReferral(cardData: CardData, pan: String) {
            iccCardHelper = ICCCardHelper()
            Looper.prepare()
            Toast.makeText(context, "Online referral", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            Timber.e("on online referral")
            Timber.e(cardData.toString())
            PasswordDialog(context, pan, object : PasswordDialog.Listener {
                override fun onConfirm(pinBlock: String?) {
                    cardData.pinBlock = pinBlock
                    iccCardHelper = ICCCardHelper(
                        customerName = "CUSTOMER",
                        cardScheme = "MasterCard",
                        cardData = cardData
                    )
                    showSelectAccountTypeDialog(context, iccCardHelper!!, liveData)
                }

                override fun onError(message: String?) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

            }).showDialog()
        }

        override fun onTransactionDeclined() {
            Looper.prepare()
            Toast.makeText(context, "transaction declined", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        override fun onApplicationEnded() {
            Looper.prepare()
            Toast.makeText(context, "application ended", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        override fun onTransactionCancelled() {
            Looper.prepare()
            Toast.makeText(context, "transaction cancelled", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        override fun logToScreen(s: String?) {

        }

    })
    NetPosApp.INSTANCE.transactionsApi.initiatePayment(object :
        PaymentDataProvider {
        override fun getPaymentDataMap(): HashMap<Int, ByteArrayWrapper> {
            val map = HashMap<Int, ByteArrayWrapper>()
            map.apply {
                put(0x9F02, ByteArrayWrapper(StringUtils.leftPad(amount.toString(), 12, '0')))
                put(
                    0x9F03,
                    ByteArrayWrapper(StringUtils.leftPad(cashBackAmount.toString(), 12, '0'))
                )
                put(0x5F2A, ByteArrayWrapper("0566"))
                put(0x9F1A, ByteArrayWrapper("0566"))
            }
            return map
        }

        override fun setPaymentDataEntry(p0: Int?, p1: ByteArrayWrapper?) {

        }

    })
    dialog.show()
}

fun sendCardEvent(s: String, s1: String, cardReaderMqttEvent: CardReaderMqttEvent) {
    val event = MqttEvent<CardReaderMqttEvent>()
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
    Timber.e("show select account dialog")
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
            Timber.e(iccCardHelper.toString())
            liveData.postValue(Event(iccCardHelper))
        }
    }
    dialogSelectAccountTypeBinding.cancelButton.setOnClickListener {
        dialog.dismiss()
        liveData.postValue(Event(ICCCardHelper(error = Throwable("Operation was canceled"))))
    }
    //dialog.show()
    iccCardHelper.apply {
        this.accountType = IsoAccountType.DEFAULT_UNSPECIFIED
    }
    Timber.e(iccCardHelper.toString())
    liveData.postValue(Event(iccCardHelper))
}
