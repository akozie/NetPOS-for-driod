package com.woleapp.netpos.contactless.model

data class ConfirmOTPResponse(
    val `data`: Data,
    val message: String,
    val status: Boolean
)