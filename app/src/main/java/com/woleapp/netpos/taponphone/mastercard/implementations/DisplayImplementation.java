package com.woleapp.netpos.taponphone.mastercard.implementations;

import android.media.AudioManager;
import android.media.ToneGenerator;

import com.mastercard.terminalsdk.listeners.DisplayProvider;
import com.mastercard.terminalsdk.listeners.TransactionProcessLogger;
import com.mastercard.terminalsdk.objects.UserInterfaceData;

public class DisplayImplementation implements DisplayProvider {

    TransactionProcessLogger logger;

    // * Implementation specific - as TransactionProcessLoggerImplementation is updating the UI,
    // to re-use those methods
    // * The instance of TransactionProcessLoggerImplementation is passed
    public DisplayImplementation(TransactionProcessLogger transactionProcessLogger) {
        logger = transactionProcessLogger;
    }

    @Override
    public void displayMessage(UserInterfaceData userInterfaceData) {

        if (userInterfaceData.getStatus() == UserInterfaceData.UIRDStatus.CARD_READ_SUCCESSFULLY) {
            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
            toneGen1.startTone(ToneGenerator.TONE_DTMF_P, 500);
        } else {
            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
            toneGen1.startTone(ToneGenerator.TONE_SUP_CONGESTION, 600);
        }

        logger.logInfo("Transaction Summary : " + userInterfaceData.toString() + "\n");

    }
}
