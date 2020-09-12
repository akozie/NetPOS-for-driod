package com.netplus.sunyardlib;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.apache.commons.lang.StringUtils;

import java.math.BigInteger;

public class NetPosUtils {

    public static String xorHex(String a, String b) {
        char[] chars = new char[a.length()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = toHex(fromHex(a.charAt(i)) ^ fromHex(b.charAt(i)));
        }
        return new String(chars);
    }

    private static int fromHex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        throw new IllegalArgumentException();
    }

    private static char toHex(int nybble) {
        if (nybble < 0 || nybble > 15) {
            throw new IllegalArgumentException();
        }
        return "0123456789ABCDEF".charAt(nybble);
    }

    private static final String TAG = NetPosUtils.class.getName();

    public static int nextTransactionCounterValue(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NetPosUtils.class.getName(), Context.MODE_PRIVATE);
        int countValue = sharedPreferences.getInt("transaction_count", 0);
        countValue++;
        sharedPreferences.edit().putInt("transaction_count", countValue).apply();
        return countValue;
    }

    public static String nextSystemAuditTraceValue(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NetPosUtils.class.getName(), Context.MODE_PRIVATE);
        int countValue = sharedPreferences.getInt("trace_audit_count", 0);
        countValue++;
        sharedPreferences.edit().putInt("trace_audit_count", countValue).apply();

        return StringUtils.leftPad(String.valueOf(countValue), 6, '0');
    }

    public static String getMerchantType() {
        //TODO pull merchant type and store locally
        return "5651";
    }

    /**
     *
     * @return
     */
    public static String getAcquiringInstitutionCode(Context context) {
        //TODO PULL AND STORE LOCALLY
        SharedPreferences preferences = context.getSharedPreferences(NetPosUtils.class.getName(), Context.MODE_PRIVATE);
        return preferences.getString("acquiring_institution_code","111132");
    }

    /**
     *
     * @param context
     * @return
     */
    public static String generateRetrievalReferenceNumber(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NetPosUtils.class.getName(), Context.MODE_PRIVATE);
        int retrievalReferenceNUmber = sharedPreferences.getInt("retrieval_reference_number", 0);
        retrievalReferenceNUmber++;
        sharedPreferences.edit().putInt("retrieval_reference_number", retrievalReferenceNUmber).apply();

        return StringUtils.leftPad(String.valueOf(retrievalReferenceNUmber), 12, '0');
    }

    /**
     *
     * @param context
     * @return
     */
    public static String getTerminalID(Context context) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(NetPosUtils.class.getName(), Context.MODE_PRIVATE);
        String terminalId = sharedPreferences.getString("terminal_id","20390060");
        return terminalId;

    }

    /**
     *
     * @param context
     * @return
     */
    public static String getCardAcceptorIDCode(Context context) {
        //TODO PULL AND STORE LOCALLY
        SharedPreferences sharedPreferences = context.getSharedPreferences(NetPosUtils.class.getName(), Context.MODE_PRIVATE);
        return sharedPreferences.getString("card_acceptor_id_cide","2057LA999997877");
    }

    /**
     *
     * @param context
     * @return
     */
    public static String getCardAcceptorNameOrLocation(Context context) {
        //TODO PULL AND STORE LOCALLY
        SharedPreferences sharedPreferences = context.getSharedPreferences(NetPosUtils.class.getName(), Context.MODE_PRIVATE);
        return sharedPreferences.getString("card_acceptor_name_or_location", "DapsSolutionsOjuelegba Lagosxxxxxxxxx NG");
    }

    /**
     *
     * @param context
     * @return
     */
    public static String getTerminalMasterKey(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NetPosUtils.class.getName(), Context.MODE_PRIVATE);
        return sharedPreferences.getString("terminal_master_key",null);
    }

    /**
     *
     * @param context
     * @param tmk
     */
    public static void storeTerminalMasterKey(Context context, String tmk) {

        Log.d(TAG, "storeTerminalMasterKey: new tmk is " + tmk);
        SharedPreferences sharedPreferences = context.getSharedPreferences(NetPosUtils.class.getName(), Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("terminal_master_key", tmk).apply();;

    }

    /**
     *
     * @param context
     * @return
     */
    public static String getTerminalSessionKey(Context context) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(NetPosUtils.class.getName(), Context.MODE_PRIVATE);
        return sharedPreferences.getString("terminal_session_key", null);

    }

    public static void storeTerminalSessionKey(Context context, String tsk) {
        Log.d(TAG, "storeTerminalSessionKey: new tsk is " + tsk);
        SharedPreferences sharedPreferences = context.getSharedPreferences(NetPosUtils.class.getName(), Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("terminal_session_key", tsk).apply();
    }

}
