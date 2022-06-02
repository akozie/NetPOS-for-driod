package com.woleapp.netpos.contactless.model

import com.google.gson.annotations.Expose

data class NipNotification(
    var beneficiaryAccountNumber: String,
    var sourceName: String,
    var sourceAccountNumber: String,
    var amount: String,
    var createdAt: String
)

data class SessionCode(@Expose var success: Boolean, @Expose var sessionCode: String)