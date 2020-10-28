package com.netplus.sunyardlib;

public abstract class CardReaderEvent {

    public static class CardDetected extends CardReaderEvent {

    }

    public static class CardRead extends CardReaderEvent {
        private CardReadResult data;

        public CardReadResult getData() {
            return data;
        }

        CardRead(CardReadResult data) {
            this.data = data;
        }
    }

}
