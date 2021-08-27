package com.woleapp.netpos.util;

import com.netpluspay.nibssclient.models.CardData;
import com.netpluspay.nibssclient.models.IsoAccountType;
import com.netpluspay.nibssclient.models.MakePaymentParams;
import com.netpluspay.nibssclient.models.TransactionType;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;



public class SampleTripleDes {

    public static void stuff(){
        MakePaymentParams makePaymentParams = new MakePaymentParams(
                0L,
                0L,
                CardData.Companion.initCardDataFromTrack(""),
                TransactionType.PURCHASE,
                IsoAccountType.DEFAULT_UNSPECIFIED
        );
        makePaymentParams.setTerminalId("");
        makePaymentParams.setRemark("");
    }

    public static String encrypt(String data, String key) {
        byte[] bData = String2Hex(data);
        byte[] bKey = String2Hex(key);
        byte[] bOutput = encrypt(bData, bKey);
        String result = Hex2String(bOutput);
        return result;
    }

    public static String decrypt(String data, String key) {
        byte[] bData = String2Hex(data);
        byte[] bKey = String2Hex(key);
        byte[] bOutput = decrypt(bData, bKey);
        String result = Hex2String(bOutput);
        return result;
    }
    public static String Hex2String(byte[] data) {
        if (data == null) {
            return "";
        } else {
            String result = "";

            for(int i = 0; i < data.length; ++i) {
                int tmp = data[i] >> 4;
                result = result + Integer.toString(tmp & 15, 16);
                tmp = data[i] & 15;
                result = result + Integer.toString(tmp & 15, 16);
            }

            return result;
        }
    }

    public static byte[] String2Hex(String data) {
        byte[] result = new byte[data.length() / 2];

        for(int i = 0; i < data.length(); i += 2) {
            result[i / 2] = (byte)Integer.parseInt(data.substring(i, i + 2), 16);
        }

        return result;
    }
    public static byte[] encrypt(byte[] data, byte[] key) {
        SecretKeySpec sk = new SecretKeySpec(GetKey(key), "DESede");

        try {
            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(1, sk);
            byte[] enc = cipher.doFinal(data);
            return enc;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException var5) {
        }

        return null;
    }

    public static byte[] decrypt(byte[] data, byte[] key) {
        SecretKeySpec sk = new SecretKeySpec(GetKey(key), "DESede");

        try {
            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(2, sk);
            byte[] enc = cipher.doFinal(data);
            return enc;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException var5) {
            var5.printStackTrace();
        }

        return null;
    }

    private static byte[] GetKey(byte[] key) {
        byte[] bKey = new byte[24];
        int i;
        if (key.length == 8) {
            for(i = 0; i < 8; ++i) {
                bKey[i] = key[i];
                bKey[i + 8] = key[i];
                bKey[i + 16] = key[i];
            }
        } else if (key.length == 16) {
            for(i = 0; i < 8; ++i) {
                bKey[i] = key[i];
                bKey[i + 8] = key[i + 8];
                bKey[i + 16] = key[i];
            }
        } else if (key.length == 24) {
            for(i = 0; i < 24; ++i) {
                bKey[i] = key[i];
            }
        }

        return bKey;
    }
}
