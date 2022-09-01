package com.woleapp.netpos.contactless.model

import android.os.Parcelable
import com.danbamitale.epmslib.entities.PosMode
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.IsoAccountType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class QrTransactionResponseFinalModel(
    val amount: Long,
    val responseCode: String,
    val currency_code: String,
    var customerName: String,
    val email: String,
    val message: String,
    val narration: String,
    val rrnOrderId: String,
    val result: String,
    val status: String,
    val transIdStan: String,
    val transmissionDateTime: String,
    val transactionTimeInMillis: Long = 0L,
    val transactionType: TransactionType = TransactionType.PURCHASE,
    val terminalId: String,
    val merchantId: String,
    var localTime_12: String = "",
    var localDate_13: String = "",
    var otherAmount: Long = 0,
    var acquiringInstCode: String = "",
    var originalForwardingInstCode: String = "",
    var authCode: String = "",
    var additionalAmount_54: String = "",
    var cardLabel: String = "",
    var cardExpiry: String = "",
    var cardHolder: String = "",
    var TVR: String = "",
    var TSI: String = "",
    var AID: String = "",
    var appCryptogram: String = "",
    var accountType: IsoAccountType = IsoAccountType.DEFAULT_UNSPECIFIED,
    var otherId: String = "",
    var id: Long = 0,
    var responseDE55: String? = null,
    var source: PosMode = PosMode.EPMS,
    var interSwitchThreshold: Long = 0L
) : Parcelable
