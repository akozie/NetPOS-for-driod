package com.woleapp.netpos.contactless.model

data class Response(
    val errors: List<Error>,
    val paymentId: String
)
