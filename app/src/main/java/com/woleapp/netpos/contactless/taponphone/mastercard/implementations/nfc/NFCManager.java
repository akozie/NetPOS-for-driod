package com.woleapp.netpos.contactless.taponphone.mastercard.implementations.nfc;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class NFCManager {
    private final Activity mActivity;
    // reader mode flags: listen for type A (not B), skipping ndef check
    public static final int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B |
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;

    @Nullable public NfcAdapter mNfcAdapter;

    public NFCManager(final Activity currentContext) {
        this.mActivity = currentContext;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mActivity);
    }

    boolean isNFCEnabled() {
        return mNfcAdapter != null && mNfcAdapter.isEnabled();
    }

    void enableNFCReaderMode(NfcAdapter.ReaderCallback readerCallback) {
        if (mNfcAdapter != null) {
            mNfcAdapter.enableReaderMode(mActivity, readerCallback, READER_FLAGS, new Bundle());
        }
    }

    void disableNFCReaderMode() {
        if (mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(mActivity);
        }
    }
}
