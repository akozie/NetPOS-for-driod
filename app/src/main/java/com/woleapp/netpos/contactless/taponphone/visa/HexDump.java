package com.woleapp.netpos.contactless.taponphone.visa;

import java.util.Locale;

public class HexDump {
    private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String toHexString(byte var0) {
        return toHexString(toByteArray(var0));
    }

    public static String toHexString(byte[] var0) {
        return toHexString(var0, 0, var0.length);
    }

    public static String toHexString(byte[] var0, int var1, int var2) {
        char[] var3 = new char[var2 * 2];
        int var4 = 0;

        for(int var5 = var1; var5 < var1 + var2; ++var5) {
            byte var6 = var0[var5];
            var3[var4++] = HEX_DIGITS[var6 >>> 4 & 15];
            var3[var4++] = HEX_DIGITS[var6 & 15];
        }

        return new String(var3);
    }

    public static String toHexString(int var0) {
        return String.format(Locale.US, "%08X", var0);
    }

    public static byte[] toByteArray(byte var0) {
        byte[] var1 = new byte[]{var0};
        return var1;
    }
}
