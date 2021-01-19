package com.netpluspay.netpossdk.emv.constant;

import com.pos.sdk.emvcore.POIEmvCoreManager.EmvTransDataConstraints;

import java.util.ArrayList;
import java.util.List;

public enum EncryptKeyType {

    ENCRYPT_KEY_TYPE_TDK(EmvTransDataConstraints.ENCRYPT_KEY_TYPE_TDK, "MK TDK"),
    ENCRYPT_KEY_TYPE_DUKPT_PIN(EmvTransDataConstraints.ENCRYPT_KEY_TYPE_DUKPT_PIN, "DuKpt Pin"),
    ENCRYPT_KEY_TYPE_DUKPT_DATA_REQUEST(EmvTransDataConstraints.ENCRYPT_KEY_TYPE_DUKPT_DATA_REQUEST, "DuKpt Request"),
    ENCRYPT_KEY_TYPE_DUKPT_DATA_RESPONSE(EmvTransDataConstraints.ENCRYPT_KEY_TYPE_DUKPT_DATA_RESPONSE, "DuKpt Response"),
    ENCRYPT_KEY_TYPE_DUKPT_MAC(EmvTransDataConstraints.ENCRYPT_KEY_TYPE_DUKPT_MAC, "DuKpt Mac");

    public final int type;

    public final String name;

    EncryptKeyType(final int type, final String name) {
        this.type = type;
        this.name = name;
    }

    public static List<String> getEncryptKeyType() {
        List<String> ret = new ArrayList<>();
        for (EncryptKeyType val : values()) {
            ret.add(val.name);
        }
        return ret;
    }

    public static int getEncryptKeyType(String name) {
        int ret = ENCRYPT_KEY_TYPE_DUKPT_PIN.type;
        for (EncryptKeyType val : values()) {
            if (val.name.equals(name)) {
                ret = val.type;
                break;
            }
        }
        return ret;
    }

    public static String getEncryptKeyType(int name) {
        String ret = ENCRYPT_KEY_TYPE_DUKPT_PIN.name;
        for (EncryptKeyType val : values()) {
            if (val.type == name) {
                ret = val.name;
                break;
            }
        }
        return ret;
    }

    public static EncryptKeyType getKeyType(int name) {
        EncryptKeyType ret = ENCRYPT_KEY_TYPE_DUKPT_PIN;
        for (EncryptKeyType val : values()) {
            if (val.type == name) {
                ret = val;
                break;
            }
        }
        return ret;
    }
}
