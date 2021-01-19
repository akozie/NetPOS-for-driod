package com.netpluspay.netpossdk.emv.constant;


import com.pos.sdk.emvcore.POIEmvCoreManager;

import java.util.ArrayList;
import java.util.List;

public enum EmvTransType {

    CASH(POIEmvCoreManager.EMV_CASH, "Cash"),
    GOODS(POIEmvCoreManager.EMV_GOODS, "Goods"),
    SERVICE(POIEmvCoreManager.EMV_SERVICE, "Service"),
    CASH_BACK(POIEmvCoreManager.EMV_CASH_BACK, "Cash Back"),
    INQUIRY(POIEmvCoreManager.EMV_INQUIRY, "Inquiry"),
    TRANSFER(POIEmvCoreManager.EMV_TRANSFER, "Transfer"),
    PAYMENT(POIEmvCoreManager.EMV_PAYMENT, "Payment"),
    ADMIN(POIEmvCoreManager.EMV_ADMIN, "Admin"),
    CASH_DEPOSIT(POIEmvCoreManager.EMV_CASH_DEPOSIT, "Cash Deposit"),
    RETURN(POIEmvCoreManager.EMV_RETURN, "Return"),
    MONEY_ADD(POIEmvCoreManager.RUPAY_MONEY_ADD, "RuPay Money Add"),
    BALANCE_ENQUIRY(POIEmvCoreManager.RUPAY_BALANCE_ENQUIRY, "RuPay Balance Enquiry"),
    VOID(POIEmvCoreManager.RUPAY_VOID, "RuPay Void"),
    SERVICE_CREATION(POIEmvCoreManager.RUPAY_SERVICE_CREATION, "RuPay Service Creation");

    private final int type;

    private final String name;

    EmvTransType(final int type, final String name) {
        this.type = type;
        this.name = name;
    }

    public static List<String> getTransType() {
        List ret = new ArrayList();
        for (EmvTransType val : values()) {
            ret.add(val.name);
        }
        return ret;
    }

    public static int getTransType(String name) {
        int ret = GOODS.type;
        for (EmvTransType val : values()) {
            if (val.name.equals(name)) {
                ret = val.type;
                break;
            }
        }
        return ret;
    }

    public static String getTransType(int name) {
        String ret = GOODS.name;
        for (EmvTransType val : values()) {
            if (val.type == name) {
                ret = val.name;
                break;
            }
        }
        return ret;
    }
}