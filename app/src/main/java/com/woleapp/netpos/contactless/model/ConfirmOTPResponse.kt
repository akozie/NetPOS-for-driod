package com.woleapp.netpos.contactless.model

data class ConfirmOTPResponse(
    val status: Boolean,
    val message: String,
    val data: Data
)