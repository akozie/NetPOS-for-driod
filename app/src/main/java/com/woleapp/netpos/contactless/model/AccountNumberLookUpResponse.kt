package com.woleapp.netpos.contactless.model

import com.woleapp.netpos.contactless.util.Event
import com.woleapp.netpos.contactless.util.Singletons
import timber.log.Timber

data class AccountNumberLookUpResponse(
    val status: Boolean,
    val phone: String,
    val account_number: String,
    val message: String
)

