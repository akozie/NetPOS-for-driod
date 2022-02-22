package com.woleapp.netpos.taponphone.implementations.nfc;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;

class ReaderCallbackImpl implements NfcAdapter.ReaderCallback {

    private TagEventListener mTagEventListener;

    ReaderCallbackImpl(final TagEventListener tagEventListener) {
        this.mTagEventListener = tagEventListener;
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        mTagEventListener.setIsoDep(IsoDep.get(tag));
    }

}
