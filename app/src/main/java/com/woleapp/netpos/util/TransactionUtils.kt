package com.woleapp.netpos.util

import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.entities.responseMessage


fun gatewayErrorTransactionResponse(amount: Long = 0, transactionType: TransactionType = TransactionType.PURCHASE, accountType: com.danbamitale.epmslib.utils.IsoAccountType = com.danbamitale.epmslib.utils.IsoAccountType.DEFAULT_UNSPECIFIED) = TransactionResponse().apply {
    this.transactionType = transactionType
    this.responseCode = "A5"
    this.RRN = "000000000000"
    this.STAN = "000000"
    this.AID = ""
    this.TSI = ""
    this.TVR = ""
    this.responseMessage
    this.transactionTimeInMillis = System.currentTimeMillis()
    this.acquiringInstCode = ""
    this.amount = amount
}
