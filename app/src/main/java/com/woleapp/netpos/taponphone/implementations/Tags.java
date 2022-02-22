package com.woleapp.netpos.taponphone.implementations;

import com.mastercard.terminalsdk.utility.ByteUtility;


public enum Tags {

    ADF_NAME("4F"),
    TAG_UPDATE_DATA("FF8111"),
    AMOUNT_AUTHORIZED_NUMERIC("9F02"),
    TRANSACTION_CURRENCY_CODE("5F2A"),
    TRANSACTION_TYPE("9C"),
    FCI_DISC_DATA("BF0C"),
    PRIORITY("87");


    private byte[] tagValueBytes;
    private int tagValueInt;

    Tags(String value) {

        tagValueBytes = ByteUtility.hexStringToByteArray(value);
        tagValueInt = ByteUtility.byteArrayToInt(tagValueBytes);
    }

    public final byte[] getTagBytes() {
        return tagValueBytes;
    }

    public final int getTag() {
        return tagValueInt;
    }
}
