package com.netpluspay.netpossdk.utils;


import com.netpluspay.netpossdk.emv.constant.EncryptKeyType;
import com.netpluspay.netpossdk.emv.constant.EncryptModeType;

public class GlobalData {

    private static final String TRANS_TYPE = "trans_type";
    private static final String TRANS_COUNTER = "trans_counter";
    private static final String TRANS_SUPPORT_APPLE = "trans_support_apple";
    private static final String TRANS_SUPPORT_ICC = "trans_support_icc";
    private static final String TRANS_SUPPORT_PICC = "trans_support_picc";
    private static final String TRANS_SUPPORT_MAG = "trans_support_mag";
    private static final String TRANS_ONLINE_RESULT = "trans_online_result";

    private static final String TRANS_ENCRYPT_OPEN = "trans_encrypt_open";
    private static final String TRANS_ENCRYPT_KEY_TYPE = "trans_encrypt_key_type";
    private static final String TRANS_ENCRYPT_KEY_INDEX = "trans_encrypt_key_index";
    private static final String TRANS_ENCRYPT_PADDING = "trans_encrypt_padding";
    private static final String TRANS_ENCRYPT_MODE = "trans_encrypt_mode";
    private static final String TRANS_ENCRYPT_VECTOR = "trans_encrypt_vector";
    private static final String TRANS_ENCRYPT_BASE64 = "trans_encrypt_base64";

    public static void setTransType(int type) {
        SPUtils.getInstance().put(TRANS_TYPE, type);
    }

    public static int getTransType() {
        return SPUtils.getInstance().getInt(TRANS_TYPE);
    }

    public static void setTransSupportAppleVas(boolean type) {
        SPUtils.getInstance().put(TRANS_SUPPORT_APPLE, type);
    }

    public static boolean isTransSupportAppleVas() {
        return SPUtils.getInstance().getBoolean(TRANS_SUPPORT_APPLE, false);
    }

    public static void setTransSupportIcc(boolean type) {
        SPUtils.getInstance().put(TRANS_SUPPORT_ICC, type);
    }

    public static boolean isTransSupportIcc() {
        return SPUtils.getInstance().getBoolean(TRANS_SUPPORT_ICC, true);
    }

    public static void setTransSupportPicc(boolean type) {
        SPUtils.getInstance().put(TRANS_SUPPORT_PICC, type);
    }

    public static boolean isTransSupportPicc() {
        return SPUtils.getInstance().getBoolean(TRANS_SUPPORT_PICC, true);
    }

    public static void setTransSupportMag(boolean type) {
        SPUtils.getInstance().put(TRANS_SUPPORT_MAG, type);
    }

    public static boolean isTransSupportMag() {
        return SPUtils.getInstance().getBoolean(TRANS_SUPPORT_MAG, true);
    }

    public static String getTransCounter() {
        int i = SPUtils.getInstance().getInt(TRANS_COUNTER, 1);
        String str = i + "";
        StringBuilder mRen = new StringBuilder();
        for (int y = str.length(); y < 4; y++) {
            mRen.append("0");
        }
        mRen.append(str);
        setTransCounter(1 + i);
        return mRen.toString();
    }

    private static void setTransCounter(int str) {
        if (str >= 9999) {
            str = 1;
        }
        SPUtils.getInstance().put(TRANS_COUNTER, str, true);
    }

    public static void setTransOnlineResult(int type) {
        SPUtils.getInstance().put(TRANS_ONLINE_RESULT, type);
    }

    public static int getTransOnlineResult() {
        return SPUtils.getInstance().getInt(TRANS_ONLINE_RESULT, 0);
    }


    public static void setTransEncryptOpen(boolean open) {
        SPUtils.getInstance().put(TRANS_ENCRYPT_OPEN, open);
    }

    public static boolean isTransEncryptOpen() {
        return SPUtils.getInstance().getBoolean(TRANS_ENCRYPT_OPEN);
    }

    public static void setTransEncryptKeyType(int type) {
        SPUtils.getInstance().put(TRANS_ENCRYPT_KEY_TYPE, type);
    }

    public static int getTransEncryptKeyType() {
        return SPUtils.getInstance().getInt(TRANS_ENCRYPT_KEY_TYPE, EncryptKeyType.ENCRYPT_KEY_TYPE_DUKPT_PIN.type);
    }

    public static void setTransEncryptKeyIndex(int index) {
        SPUtils.getInstance().put(TRANS_ENCRYPT_KEY_INDEX, index);
    }

    public static int getTransEncryptKeyIndex() {
        return SPUtils.getInstance().getInt(TRANS_ENCRYPT_KEY_INDEX);
    }

    public static void setTransEncryptPadding(String padding) {
        SPUtils.getInstance().put(TRANS_ENCRYPT_PADDING, padding);
    }

    public static String getTransEncryptPadding() {
        return SPUtils.getInstance().getString(TRANS_ENCRYPT_PADDING, "0");
    }

    public static void setTransEncryptMode(int mode) {
        SPUtils.getInstance().put(TRANS_ENCRYPT_MODE, mode);
    }

    public static int getTransEncryptMode() {
        return SPUtils.getInstance().getInt(TRANS_ENCRYPT_MODE, EncryptModeType.ENCRYPT_MODE_CBC.type);
    }

    public static void setTransEncryptVector(String vector) {
        SPUtils.getInstance().put(TRANS_ENCRYPT_VECTOR, vector);
    }

    public static String getTransEncryptVector() {
        return SPUtils.getInstance().getString(TRANS_ENCRYPT_VECTOR, "0000000000000000");
    }

    public static void setTransEncryptBase64(boolean open) {
        SPUtils.getInstance().put(TRANS_ENCRYPT_BASE64, open);
    }

    public static boolean isTransEncryptBase64() {
        return SPUtils.getInstance().getBoolean(TRANS_ENCRYPT_BASE64, true);
    }
}