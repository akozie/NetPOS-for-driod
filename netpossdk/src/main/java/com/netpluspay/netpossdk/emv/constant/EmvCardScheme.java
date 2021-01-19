package com.netpluspay.netpossdk.emv.constant;

import com.pos.sdk.utils.PosUtils;

import java.util.regex.Pattern;

public enum EmvCardScheme {
    NO_MATCH("No Match", "^3[47][0-9]{5,}$", "A0 00 00 00 25"),
    VISA("VISA", "^4[0-9]{6,}$", "A0 00 00 00 03", "A0 00 00 00 03 10 10", "A0 00 00 00 98 08 48"), //
    MASTER_CARD("Master card", "^5[1-5][0-9]{5,}$", "A0 00 00 00 04", "A0 00 00 00 05"), //
    AMERICAN_EXPRESS("American express", "^3[47][0-9]{5,}$", "A0 00 00 00 25"), //
    DISCOVER("Discover", "(6011|65|64[4-9]|622)[0-9]*", "A0 00 00 01 52 30 10"), //
    ZIP("ZIP", null, "A0 00 00 03 24"), //
    UNION_PAY("UnionPay", "^62[0-9]{14,17}", "A0 00 00 03 33"), //
    RU_PAY("RuPay", null, "A0 00 00 05 24 10 10");

    private final String[] aids;

    private final byte[][] aidsByte;

    private final String name;

    private final Pattern pattern;

    EmvCardScheme(final String pScheme, final String pRegex, final String... pAids) {
        aids = pAids;
        aidsByte = new byte[pAids.length][];
        for (int i = 0; i < aids.length; i++) {
            aidsByte[i] = PosUtils.hexStringToBytes(pAids[i]);
        }
        name = pScheme;
        if (isNotBlank(pRegex)) {
            pattern = Pattern.compile(pRegex);
        } else {
            pattern = null;
        }
    }

    public String[] getAid() {
        return aids;
    }

    public String getName() {
        return name;
    }

    public static EmvCardScheme getCardTypeByAid(final String pAid) {
        EmvCardScheme ret = NO_MATCH;
        if (pAid != null) {
            String aid = deleteWhitespace(pAid);
            for (EmvCardScheme val : values()) {
                for (String schemeAid : val.getAid()) {
                    if (aid.startsWith(deleteWhitespace(schemeAid))) {
                        ret = val;
                        break;
                    }
                }
            }
        }
        return ret;
    }

    public static EmvCardScheme getCardTypeByCardNumber(final String pCardNumber) {
        EmvCardScheme ret = NO_MATCH;
        if (pCardNumber != null) {
            for (EmvCardScheme val : values()) {
                if (val.pattern != null && val.pattern.matcher(deleteWhitespace(pCardNumber)).matches()) {
                    ret = val;
                    break;
                }
            }
        }
        return ret;
    }

    public byte[][] getAidByte() {
        return aidsByte;
    }

    private static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    private static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(cs.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    private static String deleteWhitespace(final String str) {
        if (isEmpty(str)) {
            return str;
        }
        final int sz = str.length();
        final char[] chs = new char[sz];
        int count = 0;
        for (int i = 0; i < sz; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                chs[count++] = str.charAt(i);
            }
        }
        if (count == sz) {
            return str;
        }
        return new String(chs, 0, count);
    }

    private static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
}