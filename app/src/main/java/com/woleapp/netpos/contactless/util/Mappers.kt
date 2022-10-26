package com.woleapp.netpos.contactless.util

import com.danbamitale.epmslib.entities.TransactionResponse
import com.woleapp.netpos.contactless.model.QrTransactionResponseFinalModel

object Mappers {
    fun mapQrTransToNormalTransRespType(qrTrans: QrTransactionResponseFinalModel) =
        TransactionResponse().apply {
            this.otherId =
                qrTrans.message // Since response message can not be set because it is a val from the sdk, hence i used other id to represent response message here
            transactionType = qrTrans.transactionType
            maskedPan = ""
            amount = qrTrans.amount
            transmissionDateTime = qrTrans.transmissionDateTime
            STAN = qrTrans.transIdStan
            RRN = qrTrans.rrnOrderId
            responseCode = qrTrans.responseCode
            cardLabel = qrTrans.cardLabel
            cardHolder = qrTrans.cardHolder
            transactionTimeInMillis = qrTrans.transactionTimeInMillis
            accountType = qrTrans.accountType
            terminalId = qrTrans.terminalId
            merchantId = qrTrans.merchantId
        }

    fun TransactionResponse.mapTransactionResponseToQrTransaction() {
        QrTransactionResponseFinalModel(
            amount = this.amount,
            responseCode = this.responseCode,
            currency_code = "NGN",
            customerName = this.cardHolder,
            email = "",
            message = this.otherId,
            narration = this.otherId,
            rrnOrderId = this.RRN,
            result = this.otherId,
            status = if (this.responseCode == "00") "APPROVED" else "DECLINED",
            transIdStan = this.STAN,
            transmissionDateTime = this.transmissionDateTime,
            transactionTimeInMillis = this.transactionTimeInMillis,
            transactionType = this.transactionType,
            terminalId = this.terminalId,
            merchantId = this.merchantId
        )
    }
}
