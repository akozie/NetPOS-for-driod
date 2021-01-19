package com.netpluspay.netpossdk.emv

abstract class CardReaderEvent {
    class CardDetected : CardReaderEvent()
    class CardRead internal constructor(private val data: CardReadResult) : CardReaderEvent() {
        fun getData(): CardReadResult {
            return data
        }

    }
}