package com.netplus.sunyardlib;

public class NetposException extends Throwable {

    public final int code;

    public NetposException(int code, String s) {
        super(s);
        this.code = code;
    }

}
