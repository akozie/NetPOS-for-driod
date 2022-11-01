package com.woleapp.netpos.contactless.model

data class Data(
    val account_number: String,
    val bank_id: String,
    val first_name: String,
    val last_name: String,
    val merchant_id: String,
    val phone: String,
    val terminal_id: String
)