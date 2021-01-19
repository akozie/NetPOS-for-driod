package com.netpluspay.netpossdk.emv.constant;

import com.pos.sdk.accessory.POIGeneralAPI;

import java.util.ArrayList;
import java.util.List;

public enum EncryptAppScene {

    APP_SCENE_UNENCRYPTED(POIGeneralAPI.PAYMENT_APPLICATION_SCENE_UNENCRYPTED, "Scene Unencrypted"),
    APP_SCENE_MAG_ENCRYPTED(POIGeneralAPI.PAYMENT_APPLICATION_SCENE_MAG_ENCRYPTED, "Scene MSR Encrypted"),
    APP_SCENE_ENCRYPTED(POIGeneralAPI.PAYMENT_APPLICATION_SCENE_ENCRYPTED, "Scene EMV & MSR Encrypted");

    private final int type;

    private final String name;

    EncryptAppScene(final int type, final String name) {
        this.type = type;
        this.name = name;
    }

    public static List<String> getEncryptAppScene() {
        List<String> ret = new ArrayList<>();
        for (EncryptAppScene val : values()) {
            ret.add(val.name);
        }
        return ret;
    }

    public static int getEncryptAppScene(String name) {
        int ret = APP_SCENE_UNENCRYPTED.type;
        for (EncryptAppScene val : values()) {
            if (val.name.equals(name)) {
                ret = val.type;
                break;
            }
        }
        return ret;
    }

    public static String getEncryptAppScene(int name) {
        String ret = APP_SCENE_UNENCRYPTED.name;
        for (EncryptAppScene val : values()) {
            if (val.type == name) {
                ret = val.name;
                break;
            }
        }
        return ret;
    }

    public static EncryptAppScene getAppScene(int name) {
        EncryptAppScene ret = APP_SCENE_UNENCRYPTED;
        for (EncryptAppScene val : values()) {
            if (val.type == name) {
                ret = val;
                break;
            }
        }
        return ret;
    }
}
