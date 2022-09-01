package com.woleapp.netpos.contactless.model

data class PostQrToServerVerveResponseModel(
    val amount: String,
    val code: String,
    val message: String,
    val orderId: String,
    val provider: String,
    val result: String,
    val status: String,
    val transId: String
)
