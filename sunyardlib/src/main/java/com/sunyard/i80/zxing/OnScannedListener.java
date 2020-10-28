package com.sunyard.i80.zxing;

public interface OnScannedListener{
    void onScanResult(int retCode, byte[] data);
}