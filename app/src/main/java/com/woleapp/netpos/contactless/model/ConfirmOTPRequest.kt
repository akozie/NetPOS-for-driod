package com.woleapp.netpos.contactless.model

data class ConfirmOTPRequest(
    val otp: String,
    val phone: String
)