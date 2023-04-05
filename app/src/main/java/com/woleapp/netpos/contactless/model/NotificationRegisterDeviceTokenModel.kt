package com.woleapp.netpos.contactless.model

data class NotificationRegisterDeviceTokenModel(
    val deviceRegistrationToken: String,
    val terminalId: String,
    val stormId: String
)