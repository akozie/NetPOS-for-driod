package com.woleapp.netpos.contactless.model

import android.os.Parcelable
import com.danbamitale.epmslib.utils.IsoAccountType
import com.woleapp.netpos.contactless.util.AppConstants.STRING_QR_TRANSACTION_DATE_TIME_FORMAT
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.dateStr2Long
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.getCurrentDateTime
import kotlinx.android.parcel.Parcelize

@Parcelize
data class QrTransactionResponseModel(
    val amount: String,
    val code: String,
    val currency_code: String,
    val customerName: String,
    val email: String,
    val message: String,
    val narration: String,
    val orderId: String,
    val result: String,
    val status: String,
    val transId: String
) : Parcelable {
    fun mapQrTransRespToQrRespFinalModel(
        accType: String,
        terminalId: String,
        merchantId: String
    ): QrTransactionResponseFinalModel {
        val transDateTime = getCurrentDateTime()
        return QrTransactionResponseFinalModel(
            amount.toDouble().toLong(),
            code,
            currency_code,
            customerName,
            email,
            message,
            narration,
            orderId,
            result,
            status,
            transId,
            transDateTime,
            transactionTimeInMillis = dateStr2Long(
                transDateTime,
                STRING_QR_TRANSACTION_DATE_TIME_FORMAT
            ),
            accountType = IsoAccountType.DEFAULT_UNSPECIFIED,
            terminalId = terminalId,
            merchantId = merchantId
        )
    }
}
