package com.woleapp.netpos.contactless.model

import com.woleapp.netpos.contactless.util.AppConstants.getGUID

data class PostQrToServerModel(
    val amount: Double,
    val cardData: String,
    val currency: String = "NGN",
    val merchantId: String,
    val orderId: String = getGUID(),
    val padding: String? = null,
    val naration: String
)
