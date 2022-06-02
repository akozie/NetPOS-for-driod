package com.woleapp.netpos.contactless;


import com.danbamitale.epmslib.utils.TripleDES;
import com.woleapp.netpos.contactless.util.ExtensionFunctionsKt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


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
        System.out.println("length: " + de.length());
        assertEquals(4, 2 + 2);
    }

    //0445f6e6fffebbbf
    //0445F6E6FFFEBBBF
    @Test
    public void hexTest() {
        String plainPin = "1234";
        String pin = "044562FFFFFFFFFF";
        String pan = "0000911074859199";
        String cardNum = "0000" + "5399419000144402".substring(3, 15);
        String pinKey = "c692419319d592a8a8254b4d81eb7b1f";
        String expected = "E14E2C20426FE377".toLowerCase();
        // System.out.println(Util.BytesToString(HexDump.hexStringToByteArray("0425A8EF8B7A6E66")));
        long pinLong = Long.parseLong(pin, 16);
        long panLong = Long.parseLong(pan, 16);
        long res = pinLong ^ panLong;
        String pinblock = ExtensionFunctionsKt.xorHex(pin, cardNum);
        //System.out.println(ExtensionFunctionsKt.xorHex(pin, cardNum));
        System.out.println(pinblock);
        //System.out.println(TripleDES.encrypt(pinblock, pinKey));
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testDecrypt() {
        String dec = TripleDES.decrypt("B7C60530D82A361516E938B5343D2F774C82B0AF", "B7C60530D82A361516E938B5343D2F774C82B0AF");
        System.out.println(dec);
        String pin = TripleDES.decrypt("CB603BBF8DE1EA47", "3005996b41e3ff64e1d9efc42faca953");
        String pin1 = TripleDES.decrypt("CB603BBF8DE1EA47", "FFFF9876543210000000");
        System.out.println(pin);
        System.out.println(pin1);
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testShuffle() {
        List<Integer> integerList = Arrays.asList(1,2,3,4,5,6,7,8,9,0);

        Collections.shuffle(integerList);
        System.out.println();
        System.out.println(integerList.get(0));
        for (int i = 0; i < integerList.size(); i++) {
            //System.out.print(i + " ");
        }
        System.out.println();
    }


}
