package com.woleapp.netpos.contactless.model

data class PostQrToServerResponse(
    val ACSUrl: String,
    val MD: String,
    val PaReq: String,
    val TermUrl: String,
    val amount: String,
    val code: String,
    val eciFlag: String,
    val orderId: String,
    val provider: String,
    val result: String,
    val status: String,
    val transId: String
)
