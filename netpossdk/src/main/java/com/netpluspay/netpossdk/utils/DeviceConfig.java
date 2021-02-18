package com.netpluspay.netpossdk.utils;

public class DeviceConfig {
    public static final int DEVICE_PRO = 1;
    public static final int DEVICE_MINI = 2;

    public static int Device;
    public static boolean isHardwareKeyboard;
    public static boolean isHardwarePrinter;

    public static int TPKIndex = 2;
    public static String TPKData = "00112233445566778899AABBCCDDEEFF";

    public static int TDKIndex = 1;
    public static String TDKData = "0123456789ABCDEF0123456789ABCDEF";

    public static int DUKPTIndex = 1;
    public static String DUKPTIpek = "0123456789ABCDEF0123456789ABCDEF";
    public static String DUKPTKsn = "FFFF9080102495000001";

    public static void InitDevice(int device) {
        Device = device;
        if (device == DEVICE_PRO) {
            isHardwareKeyboard = true;
            isHardwarePrinter = true;
        } else if (device == DEVICE_MINI) {
            isHardwareKeyboard = false;
            isHardwarePrinter = false;
        }
    }
}
