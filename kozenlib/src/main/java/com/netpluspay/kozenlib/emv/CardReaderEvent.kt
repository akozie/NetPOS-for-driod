package com.netpluspay.kozenlib.emv

abstract class CardReaderEvent {
    class CardDetected : CardReaderEvent()
    class CardRead internal constructor(private val data: CardReadResult) : CardReaderEvent() {
        fun getData(): CardReadResult {
            return data
        }

    }
}