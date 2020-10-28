package com.netplus.sunyardlib;

public interface ICCCardProcessHandler {

    int CLEAR_AIDS_FAILED = -1;
    int GET_ICC_DATA_FAILED = -2;
    int UNVERIFIED_RESULT = -3;
    int EMV_ERR_BASE = -4;
    int ADD_AIDS_FAILED = -5;
    int EMV_TRANS_FALLBACK = 1;
    int EMV_TRANS_TERMINATE = 2;
    int EMV_TRANS_ACCEPT = 3;
    int EMV_TRANS_DENIAL = 4;
    int EMV_TRANS_GOONLINE = 5;
    int EMV_TRANS_QPBOC_ACCEPT = 6;
    int EMV_TRANS_QPBOC_DENIAL = 7;
    int EMV_TRANS_QPBOC_GOONLINE = 8;


    void onError(int errorMessage);

    void onResult(String scanMessage);

    static String errorMessage(int code) {

        switch (code) {
            case -1:
                return "clear aids failed";
            case -2:
                return "get icc data failed";
            case -3:
                return "unverifiable result";
            case -4:
                return "emv error base";
            case -5:
                return "add aids failed";
            case 1:
                return "emv fallback";
            case 2:
                return "emv terminated";
            case 3:
                return "emv accepted";
            case 4:
                return "emv denied";
            case 5:
                return "transaction goonline";
            case 6:
                return "qboc accepted";
            case 7:
                return "qboc denied";
            case 8:
                return "qboc go online";
        }

        return "Unable to read icc data";
    }

}
