package com.woleapp.netpos;

import com.danbamitale.epmslib.utils.TripleDES;
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
        String block1 = "08E8A7AF70395D036E";
        String block2 = "0425396E7EBEBBBD";
        //String convertedByte = HexDump.toHexString(Byte.parseByte(""+sample.length() / 2));
        //System.out.println(convertedByte);
        String de = TripleDES.decrypt("08E8A7AF70395D036E", "00112233445566778899AABBCCDDEEFF");
        System.out.println("length: "+de.length());
        assertEquals(4, 2 + 2);
    }

    @Test
    public void hexTest(){
        String pin = "042539FFFFFFFFFF";
        String pan = "0000911074859199";
        String cardNum = "0000" + "4960091814144423".substring(3, 15);
        String pinKey = "c692419319d592a8a8254b4d81eb7b1f";
        String expected = "E14E2C20426FE377".toLowerCase();
       // System.out.println(Util.BytesToString(HexDump.hexStringToByteArray("0425A8EF8B7A6E66")));
        long pinLong = Long.parseLong(pin, 16);
        long panLong = Long.parseLong(pan, 16);
        long res = pinLong ^ panLong;
        String pinblock = ExtensionFunctionsKt.xorHex(pin, cardNum);
        System.out.println(ExtensionFunctionsKt.xorHex(pin, cardNum));
        System.out.println(pinblock);
        System.out.println(TripleDES.encrypt(pinblock, pinKey));
        assertEquals(4, 2 + 2);
    }
}
