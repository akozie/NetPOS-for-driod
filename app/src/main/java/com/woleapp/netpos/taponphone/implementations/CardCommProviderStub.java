package com.woleapp.netpos.taponphone.implementations;

import android.util.Log;

import com.mastercard.terminalsdk.exception.L1RSPException;
import com.mastercard.terminalsdk.listeners.CardCommunicationProvider;
import com.mastercard.terminalsdk.objects.ErrorIndication;


public class CardCommProviderStub implements CardCommunicationProvider {

    private final String TAG = this.getClass().getSimpleName();

    @Override
    public byte[] sendReceive(byte[] bytes) throws L1RSPException {
        Log.e(TAG, "sendReceive: Utilizing Stub Implementation of CardCommunicationProvider");
        throw new L1RSPException("Stub Reader", ErrorIndication.L1_Error_Code.TIMEOUT_ERROR);
    }

    @Override
    public ConnectionObject waitForCard() throws L1RSPException {
        Log.e(TAG, "connectCard: Utilizing Stub Implementation of CardCommunicationProvider");
        throw new L1RSPException("Stub Reader", ErrorIndication.L1_Error_Code.TIMEOUT_ERROR);
    }

    @Override
    public boolean removeCard() {
        return false;
    }

    @Override
    public boolean connectReader() throws L1RSPException {
        Log.e(TAG, "connectReader: Utilizing Stub Implementation of CardCommunicationProvider");
        throw new L1RSPException("Stub Reader", ErrorIndication.L1_Error_Code.PROTOCOL_ERROR);
    }

    @Override
    public boolean disconnectReader() {
        return false;
    }

    @Override
    public boolean isReaderConnected() {
        return false;
    }

    @Override
    public boolean isCardPresent() {
        return false;
    }

    @Override
    public String getDescription() {
        Log.w(TAG, "Utilizing Stub Implementation of CardCommunicationProvider");
        return "Reader Stub";
    }

    @Override
    public long getPreviousCommandExecutionTime() {
        return 0;
    }
}
