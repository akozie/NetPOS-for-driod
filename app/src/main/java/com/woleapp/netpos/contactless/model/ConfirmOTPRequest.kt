package com.woleapp.netpos.contactless.model

data class ConfirmOTPRequest(
    val phone: String,
    val otp: String
)