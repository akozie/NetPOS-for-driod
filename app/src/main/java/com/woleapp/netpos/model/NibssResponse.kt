package com.woleapp.netpos.model

import com.netpluspay.nibssclient.models.IsoAccountType
import com.netpluspay.nibssclient.models.TransactionType


data class NibssResponse(
    var AID: String,
    var RRN: String,
    var STAN: String,
    var TSI: String,
    var TVR: String,
    var accountType: IsoAccountType,
    var acquiringInstCode: String,
    var additionalAmount: Long,
    var amount: Long,
    var appCryptogram: String,
    var authCode: String,
    var cardExpiry: String,
    var cardHolder: String,
    var cardLabel: String,
    var echoData: String?,
    var errorMessage: String?,
    var id: Long,
    var localDate: String,
    var localTime: String,
    var maskedPan: String,
    var merchantId: String,
    var originalForwardingInstCode: String,
    var otherAmount: Long,
    var otherId: String,
    var responseCode: String,
    var remark: String?,
    var responseDE55: String?,
    var responseMessage: String,
    var terminalId: String,
    var transactionTimeInMillis: Long,
    var transactionType: TransactionType,
    var transmissionDateTime: String
)