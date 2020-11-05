package com.woleapp.netpos;

import com.woleapp.netpos.util.ExtensionFunctionsKt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class AnotherExampleUnitTest {

    @Test
    public void sampleTest() {
        String sample = "02394";
        //String convertedByte = HexDump.toHexString(Byte.parseByte(""+sample.length() / 2));
        //System.out.println(convertedByte);
        assertEquals(4, 2 + 2);
    }

    @Test
    public void hexTest(){
        String pin = "042539FFFFFFFFFF";
        String pan = "0000911074859199";
        String cardNum = "0000" + "4960091814144423".substring(3, 15);
       // System.out.println(Util.BytesToString(HexDump.hexStringToByteArray("0425A8EF8B7A6E66")));
        long pinLong = Long.parseLong(pin, 16);
        long panLong = Long.parseLong(pan, 16);
        long res = pinLong ^ panLong;
        System.out.println(ExtensionFunctionsKt.xorHex(pin, cardNum));
        assertEquals(4, 2 + 2);
    }
}
