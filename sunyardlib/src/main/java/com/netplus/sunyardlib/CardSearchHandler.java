package com.netplus.sunyardlib;

public interface CardSearchHandler {

    int READ_FAIL = -3;
    int UNKNOWN_CARD = -2;
    int TIMEOUT = -1;
    int EMV_API_CHANNEL_FROM_ICC = 0;

    void onError(int errorMessage);

    void onResult(String scanMessage);

    static String errorMessage(int code) {

        switch (code) {
            case -3:
                return "card read failed";
            case -2:
                return "unknown card";
            case  -1:
                return "card read time out";
            default:
                return "Card read failed";
        }

    }


}
