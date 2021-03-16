//package com.netpluspay.netpossdk.utils;
//
//import android.graphics.Bitmap;
//
//import com.google.zxing.BarcodeFormat;
//import com.google.zxing.EncodeHintType;
//import com.google.zxing.MultiFormatWriter;
//import com.google.zxing.WriterException;
//import com.google.zxing.common.BitMatrix;
//import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
//
//import java.util.Hashtable;
//
//public class QRUtil {
//    private static final String CODE = "utf-8";
//    private static final int BLACK = 0xff000000;
//    private static final int WHITE = 0xffffffff;
//
//    public static Bitmap getQrBmp(String str, Integer height) {
//        if (height == null) {
//            height = 240;
//        }
//        if (height > 400) {
//            height = 400;
//        }
//        height = height - height % 8;
//        try {
//            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
//            hints.put(EncodeHintType.CHARACTER_SET, CODE);
//            hints.put(EncodeHintType.MARGIN, 0);
//            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
//            BitMatrix bitMatrix = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, height, height, hints);
//            return toBufferedImageBMP(bitMatrix);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public static Bitmap getBarcodeBmp(String str, Integer width, Integer height) {
//        if (width == null) {
//            width = 240;
//        }
//        if (width > 400) {
//            width = 400;
//        }
//        width = width - width % 8;
//        try {
//            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
//            hints.put(EncodeHintType.CHARACTER_SET, CODE);
//            hints.put(EncodeHintType.MARGIN, 0);
//            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
//            BitMatrix bitMatrix = new MultiFormatWriter().encode(str, BarcodeFormat.CODE_128, width, height, hints);
//            return toBufferedImageBMP(bitMatrix);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public static Bitmap createQRCode(String str, int widthAndHeight) {
//        if (widthAndHeight > 400) {
//            widthAndHeight = 400;
//        }
//        widthAndHeight = widthAndHeight - widthAndHeight % 8;
//        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
//        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
//        hints.put(EncodeHintType.MARGIN, 0);
//        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
//        BitMatrix matrix = null;
//        try {
//            matrix = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, widthAndHeight, widthAndHeight);
//        } catch (WriterException e) {
//            e.printStackTrace();
//        }
//        int width = matrix.getWidth();
//        int height = matrix.getHeight();
//        int[] pixels = new int[width * height];
//
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                if (matrix.get(x, y)) {
//                    pixels[y * width + x] = BLACK;
//                } else {
//                    pixels[y * width + x] = WHITE;
//                }
//            }
//        }
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
//        return bitmap;
//    }
//
//    public static Bitmap createQRCode(String str, int widthAndHeight, BarcodeFormat format) {
//        if (widthAndHeight > 400) {
//            widthAndHeight = 400;
//        }
//        widthAndHeight = widthAndHeight - widthAndHeight % 8;
//        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
//        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
//        hints.put(EncodeHintType.MARGIN, 0);
//        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
//        BitMatrix matrix = null;
//        try {
//            matrix = new MultiFormatWriter().encode(str, format, widthAndHeight, widthAndHeight);
//        } catch (WriterException e) {
//            e.printStackTrace();
//        }
//        int width = matrix.getWidth();
//        int height = matrix.getHeight();
//        int[] pixels = new int[width * height];
//
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                if (matrix.get(x, y)) {
//                    pixels[y * width + x] = BLACK;
//                } else {
//                    pixels[y * width + x] = WHITE;
//                }
//            }
//        }
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
//        return bitmap;
//    }
//
//    private static Bitmap toBufferedImageBMP(BitMatrix matrix) {
//        int width = matrix.getWidth();
//        int height = matrix.getHeight();
//
//        if (width % 8 != 0) {
//            return null;
//        }
//        int[] pixels = new int[width * height];
//        Bitmap bitmap;
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                if (matrix.get(x, y)) {
//                    pixels[y * width + x] = 0xff000000;
//                } else {
//                    pixels[y * width + x] = 0xffffffff;
//                }
//            }
//        }
//        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
//        return bitmap;
//    }
//}
