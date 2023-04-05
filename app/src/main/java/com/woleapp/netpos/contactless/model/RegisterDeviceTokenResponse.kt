package com.woleapp.netpos.contactless.model

data class RegisterDeviceTokenResponse(
    val `data`: List<Int>,
    val message: String,
    val success: Boolean
)