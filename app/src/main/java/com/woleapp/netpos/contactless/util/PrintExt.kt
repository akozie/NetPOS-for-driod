package com.woleapp.netpos.contactless.util

import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.responseMessage
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.woleapp.netpos.contactless.model.QrTransactionResponseFinalModel

fun TransactionResponse.buildSMSText(s: String? = null): StringBuilder = StringBuilder().apply {
    append("POS $transactionType ${if (responseCode == "00") "Approved" else "Declined"}\n\n")
    append("Response Code: $responseCode\n")
    append(
        "Message: ${
        try {
            if (responseMessage.contains("length=0; index=-1")) "Processing Error" else responseMessage
        } catch (e: java.lang.Exception) {
            ""
        }
        }\n"
    )
    append("Amount: ${amount.div(100).formatCurrencyAmount("\u20A6")}\n")
    if (otherAmount > 0) {
        append("Cashback Amount: ${otherAmount.div(100).formatCurrencyAmount("\u20A6")}\n")
    }
    append("Date/Time: ${transactionTimeInMillis.formatDate()}\n")
    s?.let {
        append("Remark: $it\n")
    }
    append("Auth Code: $authCode\n")
    append("RRN: $RRN\n")
    append("STAN: $STAN\n")
    append("Card: $cardLabel - $maskedPan\n")
    append("Card Owner: $cardHolder\n")
    append("Merchant: ${Singletons.getCurrentlyLoggedInUser()?.business_name}\n")
    append("Terminal ID: $terminalId\n")
}

fun QrTransactionResponseFinalModel.buildSMSTextForQrTransaction(): StringBuilder =
    StringBuilder().apply {
        append("QR Purchase ${if (responseCode == "00") "Approved" else "Declined"}\n\n")
        append("Response Code: $responseCode\n")
        append(
            "Message: ${
            try {
                if (message.contains("length=0; index=-1")) "Processing Error" else message
            } catch (e: java.lang.Exception) {
                ""
            }
            }\n"
        )
        append("Narration: $narration\n")
        append("Amount: ${amount.toDouble().formatCurrencyAmount("\u20A6")}\n")
        append("Date/Time: $transmissionDateTime\n")
//    append("Auth Code: $authCode\n")
        append("OrderId: $rrnOrderId\n")
        append("Transaction Id: $transIdStan\n")
//    append("Card: $cardLabel - $maskedPan\n")
        append("Card Owner: $cardHolder\n")
        append("Merchant: ${Singletons.getCurrentlyLoggedInUser()?.business_name}\n")
        append("Terminal ID: ${Singletons.getCurrentlyLoggedInUser()?.terminal_id}\n")
    }

fun TransactionResponse.builder() = StringBuilder().apply {
    append("Merchant Name: ").append(Singletons.getCurrentlyLoggedInUser()!!.business_name)
    append("\nTERMINAL ID: ").append(terminalId).append("\n")
    append(transactionType).append("\n")
    append("DATE/TIME: ").append(transactionTimeInMillis.formatDate()).append("\n")
    append("AMOUNT: ").append(amount.div(100).formatCurrencyAmount("\u20A6")).append("\n")
    if (maskedPan.isNotEmpty()) {
        append(cardLabel).append(" Ending with ").append(maskedPan.substring(maskedPan.length - 4))
            .append("\n")
    } else {
        append(cardLabel).append("\n")
    }
    append("RESPONSE CODE: ").append(responseCode).append("\n").append(
        " : ${
        try {
            responseMessage
        } catch (ex: Exception) {
            "Error"
        }
        }"
    )
}
