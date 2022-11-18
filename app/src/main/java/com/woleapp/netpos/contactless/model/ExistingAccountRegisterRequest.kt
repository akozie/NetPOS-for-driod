package com.woleapp.netpos.contactless.model

data class ExistingAccountRegisterRequest(
    val accountNumber: String,
    val businessAddress: String,
    val businessName: String,
    val contactInformation: String,
    val username: String,
    val password: String,
    val phoneNumber: String,
    val terminalId: String,
    val merchantId: String
)