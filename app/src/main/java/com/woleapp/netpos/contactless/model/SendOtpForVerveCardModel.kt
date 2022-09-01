package com.woleapp.netpos.contactless.model

data class SendOtpForVerveCardModel(
    val OTPData: String,
    val type: String = "OTP"
)
