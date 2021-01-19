package com.netpluspay.netpossdk.utils;

import java.util.Locale;

public class HexDump {
    private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public HexDump() {
    }

    public static String dumpHexString(byte[] var0) {
        return dumpHexString(var0, 0, var0.length);
    }

    public static String dumpHexString(byte[] var0, int var1, int var2) {
        StringBuilder var3 = new StringBuilder();
        byte[] var4 = new byte[16];
        int var5 = 0;
        var3.append("\n0x");
        var3.append(toHexString(var1));

        int var6;
        int var7;
        for(var6 = var1; var6 < var1 + var2; ++var6) {
            if (var5 == 16) {
                var3.append(" ");
                var7 = 0;

                while(true) {
                    if (var7 >= 16) {
                        var3.append("\n0x");
                        var3.append(toHexString(var6));
                        var5 = 0;
                        break;
                    }

                    if (var4[var7] > 32 && var4[var7] < 126) {
                        var3.append(new String(var4, var7, 1));
                    } else {
                        var3.append(".");
                    }

                    ++var7;
                }
            }

            byte var8 = var0[var6];
            var3.append(" ");
            var3.append(HEX_DIGITS[var8 >>> 4 & 15]);
            var3.append(HEX_DIGITS[var8 & 15]);
            var4[var5++] = var8;
        }

        if (var5 != 16) {
            var6 = (16 - var5) * 3;
            ++var6;

            for(var7 = 0; var7 < var6; ++var7) {
                var3.append(" ");
            }

            for(var7 = 0; var7 < var5; ++var7) {
                if (var4[var7] > 32 && var4[var7] < 126) {
                    var3.append(new String(var4, var7, 1));
                } else {
                    var3.append(".");
                }
            }
        }

        return var3.toString();
    }

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

    public static byte[] toByteArray(int var0) {
        byte[] var1 = new byte[]{(byte)(var0 >> 24 & 255), (byte)(var0 >> 16 & 255), (byte)(var0 >> 8 & 255), (byte)(var0 & 255)};
        return var1;
    }

    private static int toByte(char var0) {
        if (var0 >= '0' && var0 <= '9') {
            return var0 - 48;
        } else if (var0 >= 'A' && var0 <= 'F') {
            return var0 - 65 + 10;
        } else if (var0 >= 'a' && var0 <= 'f') {
            return var0 - 97 + 10;
        } else {
            throw new RuntimeException("Invalid hex char '" + var0 + "'");
        }
    }

    public static byte[] hexStringToByteArray(String var0) {
        int var1 = var0.length();
        byte[] var2 = new byte[var1 / 2];

        for(int var3 = 0; var3 < var1; var3 += 2) {
            var2[var3 / 2] = (byte)(toByte(var0.charAt(var3)) << 4 | toByte(var0.charAt(var3 + 1)));
        }

        return var2;
    }

    public static byte[] hexStringToByteArray64(String var0) {
        int var1 = var0.length();
        byte[] var2 = new byte[64];

        for(int var3 = 0; var3 < var1; var3 += 2) {
            var2[var3 / 2] = (byte)(toByte(var0.charAt(var3)) << 4 | toByte(var0.charAt(var3 + 1)));
        }

        return var2;
    }

    public static int bytesToInt(byte[] var0, int var1) {
        int[] var3 = new int[]{var0[var1 + 3] & 255, (var0[var1 + 2] & 255) << 8, (var0[var1 + 1] & 255) << 16, (var0[var1] & 255) << 24};
        int var2 = var3[0] | var3[1] | var3[2] | var3[3];
        return var2;
    }

    public static byte[] intToBytes2(int var0) {
        byte[] var1 = new byte[]{(byte)(var0 >> 24 & 255), (byte)(var0 >> 16 & 255), (byte)(var0 >> 8 & 255), (byte)(var0 & 255)};
        return var1;
    }

    public static int bytesToint(byte[] var0, int var1) {
        int var2 = var0[var1] & 255 | (var0[var1 + 1] & 255) << 8 | (var0[var1 + 2] & 255) << 16 | (var0[var1 + 3] & 255) << 24;
        return var2;
    }

    public static byte[] intToBytes(int var0) {
        byte[] var1 = new byte[]{(byte)(var0 & 255), (byte)(var0 >> 8 & 255), (byte)(var0 >> 16 & 255), (byte)(var0 >> 24 & 255)};
        return var1;
    }

    public static String hexStr2Str(String var0) {
        String var1 = "0123456789ABCDEF";
        char[] var2 = var0.toCharArray();
        byte[] var3 = new byte[var0.length() / 2];

        for(int var5 = 0; var5 < var3.length; ++var5) {
            int var4 = var1.indexOf(var2[2 * var5]) * 16;
            var4 += var1.indexOf(var2[2 * var5 + 1]);
            var3[var5] = (byte)(var4 & 255);
        }

        return new String(var3);
    }
}
