package com.netpluspay.netpossdk.emv

sealed class CardReaderEvent {
    class CardDetected(val mode : Int) : CardReaderEvent()
    class CardRead internal constructor(val data: CardReadResult) : CardReaderEvent()
}