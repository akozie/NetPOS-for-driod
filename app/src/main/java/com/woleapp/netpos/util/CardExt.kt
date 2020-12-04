@file:Suppress("DEPRECATION")

package com.woleapp.netpos.util

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Handler
import android.view.LayoutInflater
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.utils.IsoAccountType
import com.netpluspay.kozenlib.emv.CardReadResult
import com.netpluspay.kozenlib.emv.CardReaderEvent
import com.netpluspay.kozenlib.emv.CardReaderService
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.DialogSelectAccountTypeBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

data class ICCCardHelper(
    val cardReadResult:CardReadResult? = null,
    val customerName: String? = null,
    val cardScheme: String? = null,
    var accountType: IsoAccountType? = null,
    val cardData: CardData? = null,
    val error: Throwable? = null
)

fun showCardDialog(
    context: Activity,
    amount: Long,
    cashBackAmount: Long
): LiveData<Event<ICCCardHelper>> {
    val liveData = MutableLiveData<Event<ICCCardHelper>>()
    var hasCardBeenRead = false
    val dialog = ProgressDialog(context)
        .apply {
            setMessage("Waiting for card")
            setCancelable(false)
        }
    var iccCardHelper: ICCCardHelper? = null
    val c = CardReaderService(context).initiateICCCardPayment(
        amount,
        cashBackAmount
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
                    iccCardHelper = ICCCardHelper(
                        cardReadResult = cardResult,
                        customerName = cardResult.cardHolderName,
                        cardScheme = cardResult.cardScheme,
                        cardData = card
                    )
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
                liveData.value = Event(ICCCardHelper(error = it))
            }

        }, {
            dialog.dismiss()
            showSelectAccountTypeDialog(context, iccCardHelper!!, liveData)
        })

    Handler().postDelayed({
        if (!hasCardBeenRead) {
            liveData.value =
                Event(ICCCardHelper(error = Throwable("Timed out while waiting for card")))
            c.dispose()
            dialog.dismiss()
        }
    }, 45000)
    dialog.show()
    return liveData
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
