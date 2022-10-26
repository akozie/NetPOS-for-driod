package com.woleapp.netpos.contactless.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VerveTransactionResponse(
    val amount: String,
    val code: String,
    val message: String,
    val orderId: String,
    val result: String,
    val status: String,
    val transId: String
) : Parcelable {
    fun mapToQrTransactionResponseModel() =
        QrTransactionResponseModel(
            amount,
            code,
            "NGN",
            "CUSTOMER",
            "",
            message,
            "",
            orderId,
            result,
            status,
            transId
        )
}
