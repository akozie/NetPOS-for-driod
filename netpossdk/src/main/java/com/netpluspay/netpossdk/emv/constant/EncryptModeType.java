package com.netpluspay.netpossdk.emv.constant;

import com.pos.sdk.emvcore.POIEmvCoreManager.EmvTransDataConstraints;

import java.util.ArrayList;
import java.util.List;

public enum EncryptModeType {

    ENCRYPT_MODE_CBC(EmvTransDataConstraints.ENCRYPT_MODE_CBC, "CBC"),
    ENCRYPT_MODE_EBC(EmvTransDataConstraints.ENCRYPT_MODE_ECB, "EBC");

    public final int type;

    public final String name;

    EncryptModeType(final int type, final String name) {
        this.type = type;
        this.name = name;
    }

    public static List<String> getEncryptMode() {
        List<String> ret = new ArrayList<>();
        for (EncryptModeType val : values()) {
            ret.add(val.name);
        }
        return ret;
    }

    public static int getEncryptMode(String name) {
        int ret = ENCRYPT_MODE_CBC.type;
        for (EncryptModeType val : values()) {
            if (val.name.equals(name)) {
                ret = val.type;
                break;
            }
        }
        return ret;
    }

    public static String getEncryptMode(int name) {
        String ret = ENCRYPT_MODE_CBC.name;
        for (EncryptModeType val : values()) {
            if (val.type == name) {
                ret = val.name;
                break;
            }
        }
        return ret;
    }

    public static EncryptModeType getMode(int name) {
        EncryptModeType ret = ENCRYPT_MODE_CBC;
        for (EncryptModeType val : values()) {
            if (val.type == name) {
                ret = val;
                break;
            }
        }
        return ret;
    }
}
