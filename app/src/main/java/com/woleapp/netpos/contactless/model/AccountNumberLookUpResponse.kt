package com.woleapp.netpos.contactless.model

data class AccountNumberLookUpResponse(
    val status: Boolean,
    val phone: String,
    val message: String
)