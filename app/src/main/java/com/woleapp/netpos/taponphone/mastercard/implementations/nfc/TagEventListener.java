package com.woleapp.netpos.taponphone.mastercard.implementations.nfc;

import android.nfc.tech.IsoDep;

class TagEventListener {
    /**
     * ISO DEP object to access ISO-DEP (ISO 14443-4) properties and I/O operations on a Tag
     */
    private IsoDep mIsoDep;

    IsoDep getIsoDep() {
        return mIsoDep;
    }

    void setIsoDep(final IsoDep isoDep) {
        mIsoDep = isoDep;
    }
}
