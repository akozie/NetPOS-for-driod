package com.woleapp.netpos.model

import com.netpluspay.nibssclient.models.CardData


data class Logger(
    var cardData: CardData,
    val manualEncryptedPinBlock: String,
    val plainPinBlock: String,
    val cardScheme: String,
    val cardHolderName: String,
    val loadedPinKey: String,
    val manualPinKey: String,
    val terminalId: String
)