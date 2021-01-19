package com.netpluspay.netpossdk.emv.constant;


import com.pos.sdk.emvcore.PosEmvErrCode;

public enum EmvErrCodeType {

    APPROVED(PosEmvErrCode.EMV_APPROVED, "Transaction Approved"),
    APPROVED_ONLINE(PosEmvErrCode.EMV_APPROVED_ONLINE, "Transaction Online Approved"),
    DECLINED(PosEmvErrCode.EMV_DECLINED, "Transaction Declined"),
    FORCE_APPROVED(PosEmvErrCode.EMV_FORCE_APPROVED, "Transaction Force Approved"),
//    VAS_APPROVED(PosEmvErrCode.APPLE_VAS_APPROVED, "Apple Vas Approved"),

    OTHER_INTERFACE(PosEmvErrCode.EMV_OTHER_INTERFACE, "Other Interface"),
    COMMAND_FAIL(PosEmvErrCode.EMV_COMMAND_FAIL, "Command Fail"),
    CARD_BLOCK(PosEmvErrCode.EMV_CARD_BLOCK, "Card Block"),
    APP_EMPTY(PosEmvErrCode.EMV_APP_EMPTY, "No Application"),
    APP_BLOCK(PosEmvErrCode.EMV_APP_BLOCK, "Application Block"),
    FALLBACK(PosEmvErrCode.EMV_FALLBACK, "Fallback"),
    SEE_PHONE(PosEmvErrCode.EMV_SEE_PHONE, "See Phone"),
    NOT_ALLOWED(PosEmvErrCode.EMV_NOT_ALLOWED, "Not Allowed"),
    NOT_ACCEPTED(PosEmvErrCode.EMV_NOT_ACCEPTED, "Not Accepted"),
    TERMINATED(PosEmvErrCode.EMV_TERMINATED, "Terminated"),
    CANCEL(PosEmvErrCode.EMV_CANCEL, "Cancel"),
    TIMEOUT(PosEmvErrCode.EMV_TIMEOUT, "Timeout"),
    MULTI_PICC(PosEmvErrCode.EMV_MULTI_PICC, "Multi Picc"),
    ICC_INTERFACE(PosEmvErrCode.EMV_ICC_INTERFACE, "ICC Interface"),
    EMV_OTHER_ERROR(PosEmvErrCode.EMV_OTHER_ERROR, "Other Error");

//    APPLE_VAS_FALL(PosEmvErrCode.APPLE_VAS_FALL, "Apple Vas Failure");


    private final int type;

    private final String name;

    EmvErrCodeType(final int type, final String name) {
        this.type = type;
        this.name = name;
    }

    public static int getErrCodeType(String name) {
        int ret = OTHER_INTERFACE.type;
        for (EmvErrCodeType val : values()) {
            if (val.name.equals(name)) {
                ret = val.type;
                break;
            }
        }
        return ret;
    }

    public static String getErrCodeType(int name) {
        String ret = OTHER_INTERFACE.name;
        for (EmvErrCodeType val : values()) {
            if (val.type == name) {
                ret = val.name;
                break;
            }
        }
        return ret;
    }
}