package com.netpluspay.netpossdk.core;

public abstract class AndroidTerminalCardReaderFactory<T> {
    /**
     * @param amount         Amount to be charged
     * @param cashBackAmount Cash Back amount if applicable else pass 0.
     */
    public abstract T initiateICCCardPayment(long amount, long cashBackAmount);

    /*
     * method called after initiateICCCardPayment to scan for cards.
     * */
    protected abstract void scanForCard();

    protected abstract void showPinPad();

    /*
     * method called after a card has been detected to read data from the detected card.
     * */
    protected abstract void readCard();

    protected abstract void updateICCardData(int cardReadResultCode);
}
