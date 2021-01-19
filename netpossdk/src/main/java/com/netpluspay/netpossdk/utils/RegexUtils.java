package com.netpluspay.netpossdk.utils;

public class RegexUtils {

    public static final String PASSWORD_REGEX = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,20}$";

    public static final String IP_REGEX = "^((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))$";

    public static final String PHONE_NUMBER_REGEX = "^1{1}(3{1}\\d{1}|5{1}[012356789]{1}|8{1}[0123456789]{1}|7{1}[0123678]{1})\\d{8}$";

    public static final String EMAIL_REGEX = "^(www\\.)?\\w+@\\w+(\\.\\w+)+$";

    public static final String CHINESE_REGEX = "^[\u4e00-\u9f5a]+$";

    public static final String POSITIVE_INTEGER_REGEX = "^\\d+$";

    public static final String ID_CARD = "^(^[1-9]\\d{7}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}$)|(^[1-9]\\d{5}[1-9]\\d{3}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])((\\d{4})|\\d{3}[Xx])$)$";

    public static final String ZIP_CODE = "^\\d{6}$";

    public static final String URL = "^(([hH][tT]{2}[pP][sS]?)|([fF][tT][pP]))\\:\\/\\/[wW]{3}\\.[\\w-]+\\.\\w{2,4}(\\/.*)?$";

    public static final String NUMBER = "^[0-9]{2,}$";

    public static final String CHARACTER = "^[A-Za-z0-9_\\s]+$";

    public static final String AID_RID = "^(([[a|b|d|A|B|D|5][0]{1,8}[1-9]+]{10,32})|([[501649FF20][0-9]*]{10,32}))$";//

    public static boolean isEmail(String string) {
        return string.matches(EMAIL_REGEX);
    }

    public static boolean isMobilePhoneNumber(String string) {
        return string.matches(PHONE_NUMBER_REGEX);
    }

    public static boolean isIp(String string) {
        return string.matches(IP_REGEX);
    }

    public static boolean isChinese(String string) {
        return string.matches(CHINESE_REGEX);
    }

    public static boolean isPositiveInteger(String string) {
        return string.matches(POSITIVE_INTEGER_REGEX);
    }

    public static boolean isIdCard(String string) {
        return string.matches(ID_CARD);
    }

    public static boolean isZipCode(String string) {
        return string.matches(ZIP_CODE);
    }

    public static boolean isURL(String string) {
        return string.matches(URL);
    }

    public static boolean isPassword(String string) {
        return string.matches(PASSWORD_REGEX);
    }

    public static boolean isNumber(String string) {
        return string.matches(NUMBER);
    }

    public static boolean isCharacter(String string) {
        return string.matches(CHARACTER);
    }

    public static boolean isAIDFormat(String string) {
        return string.matches(AID_RID);
    }

}
