package com.netpluspay.netpossdk.emv.constant;


import com.pos.sdk.emvcore.POIEmvCoreManager.EmvOnlineResultConstraints;

import java.util.ArrayList;
import java.util.List;

public enum EmvOnlineResult {

    APPROVE(EmvOnlineResultConstraints.EMV_ONLINE_APPROVE, "Online Approve"),
    FAIL(EmvOnlineResultConstraints.EMV_ONLINE_FAIL, "Online Fail"),
    DENIAL(EmvOnlineResultConstraints.EMV_ONLINE_DENIAL, "Online Denial");

    private final int type;

    private final String name;

    EmvOnlineResult(final int type, final String name) {
        this.type = type;
        this.name = name;
    }

    public static List<String> getOnlineResult() {
        List ret = new ArrayList();
        for (EmvOnlineResult val : values()) {
            ret.add(val.name);
        }
        return ret;
    }

    public static int getOnlineResult(String name) {
        int ret = APPROVE.type;
        for (EmvOnlineResult val : values()) {
            if (val.name.equals(name)) {
                ret = val.type;
                break;
            }
        }
        return ret;
    }

    public static String getOnlineResult(int name) {
        String ret = APPROVE.name;
        for (EmvOnlineResult val : values()) {
            if (val.type == name) {
                ret = val.name;
                break;
            }
        }
        return ret;
    }
}