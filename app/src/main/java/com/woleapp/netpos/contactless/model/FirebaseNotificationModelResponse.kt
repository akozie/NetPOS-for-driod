package com.woleapp.netpos.contactless.model

data class FirebaseNotificationModelResponse(
    val amount: String,
    val code: String,
    val currency_code: String,
    val customerName: String,
    val email: String,
    val maskedPan: String,
    val merchantId: String,
    val message: String,
    val orderId: String,
    val otherId: String,
    val provider: String,
    val responseMessage: String,
    val result: String,
    val rrn: String,
    val status: String,
    val terminalId: String,
    val transId: String
)