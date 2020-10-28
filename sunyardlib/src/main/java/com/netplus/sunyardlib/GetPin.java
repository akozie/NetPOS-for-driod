package com.netplus.sunyardlib;

import com.socsi.smartposapi.emv2.EmvL2;

public interface GetPin {

    void getPin(byte[] pan, EmvL2.GetPinHandler getPinHandler);

}
