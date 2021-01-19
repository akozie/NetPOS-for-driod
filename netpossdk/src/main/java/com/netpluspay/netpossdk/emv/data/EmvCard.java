package com.netpluspay.netpossdk.emv.data;

import android.util.Log;


import com.netpluspay.netpossdk.utils.tlv.BerTag;
import com.netpluspay.netpossdk.utils.tlv.BerTlv;
import com.netpluspay.netpossdk.utils.tlv.BerTlvBuilder;
import com.netpluspay.netpossdk.utils.tlv.BerTlvParser;
import com.netpluspay.netpossdk.utils.tlv.BerTlvs;
import com.netpluspay.netpossdk.utils.tlv.HexUtil;
import com.netpluspay.netpossdk.emv.constant.EmvCardScheme;


import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmvCard implements Serializable
{
    /**
     * Emv tlv data
     */
    private byte[] tlvData;

    /**
     * Card type
     */
    private EmvCardScheme type;

    /**
     * Track 2 data
     */
    private EmvTrack2 track2;

    /**
     * Track 1 data
     */
    private EmvTrack1 track1;

    /**
     * Card number
     */
    private String cardNumber;

    /**
     * Card expiration date
     */
    private Date cardExpireDate;

    /**
     * Card sequence number
     */
    private int cardSequenceNumber = -1;

    /**
     * Card holder name
     */
    private String cardHolderName;

    /**
     * Application label
     */
    private String appLabel;

    /**
     * Service code
     */
    private String serviceCode;

    /**
     * Issuer Country Code
     */
    private int issuerCountryCode = -1;


    public EmvCard(byte[] data) {
        this.tlvData = data;
        BerTlvParser mTlvParser = new BerTlvParser();
        BerTlvs mTlvs = mTlvParser.parse(tlvData);
        BerTlv mEmvTag = mTlvs.find(new BerTag("5F24"));
        if (mEmvTag != null) {
            Date date = null;
            DateFormat format = new SimpleDateFormat("yyMMdd", Locale.getDefault());
            try {
                date = format.parse(mEmvTag.getHexValue());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            cardExpireDate = date;
        }

        mEmvTag = mTlvs.find(new BerTag("5F20"));
        if (mEmvTag != null) {
            cardHolderName = mEmvTag.getTextValue();
        }

        mEmvTag = mTlvs.find(new BerTag("5F28"));
        if (mEmvTag != null) {
            issuerCountryCode = Integer.parseInt(mEmvTag.getHexValue());
        }

        mEmvTag = mTlvs.find(new BerTag("9F12"));
        if (mEmvTag == null) {
            mEmvTag = mTlvs.find(new BerTag("50"));
        }
        if (mEmvTag != null) {
            appLabel = mEmvTag.getTextValue();
        }

        mEmvTag = mTlvs.find(new BerTag("5A"));
        if (mEmvTag != null) {
            cardNumber = mEmvTag.getHexValue();
        }

        mEmvTag = mTlvs.find(new BerTag("5F34"));
        if (mEmvTag != null) {
            cardSequenceNumber = mEmvTag.getIntValue();
        }

        mEmvTag = mTlvs.find(new BerTag("5F30"));
        if (mEmvTag != null) {
            serviceCode = mEmvTag.getHexValue();
        }

        mEmvTag = mTlvs.find(new BerTag("56"));
        if (mEmvTag != null) {
            track1 = extractTrack1Data(mEmvTag.getBytesValue());
        }

        mEmvTag = mTlvs.find(new BerTag("57"));

        if (mEmvTag == null) {
            mEmvTag = mTlvs.find(new BerTag("9F20"));
        }

        if (mEmvTag == null) {
            mEmvTag = mTlvs.find(new BerTag("9F6B"));
        }

        if (mEmvTag != null) {
            track2 = extractTrack2EquivalentData(mEmvTag.getBytesValue());
        }

        if (track2 != null) {
            if (cardNumber == null)
                cardNumber = track2.getCardNumber();

            if (cardExpireDate == null)
                cardExpireDate = track2.getExpireDate();

            if (serviceCode == null)
                serviceCode = track2.getService();
        }

        if (track1 != null) {
            if (cardNumber == null)
                cardNumber = track1.getCardNumber();

            if (cardExpireDate == null)
                cardExpireDate = track1.getExpireDate();

            if (serviceCode == null)
                serviceCode = track1.getService();

            if (cardHolderName == null)
                cardHolderName = track1.getCardHolderName();
        }

        if (cardNumber != null) {
            int panIndex = cardNumber.toUpperCase().indexOf('F');
            if (panIndex != -1) {
                cardNumber = cardNumber.substring(0, panIndex);
            }
        }

        mEmvTag = mTlvs.find(new BerTag("9F06"));
        if (mEmvTag != null) {
            type = findCardScheme(mEmvTag.getHexValue(), cardNumber);
        } else {
            type = findCardScheme(null, cardNumber);
        }
    }

    public byte[] getTlvData(String[] listTag) {
        BerTlvBuilder berTlvBuilder = new BerTlvBuilder();
        BerTlvs berTlvs = new BerTlvParser().parse(tlvData);
        for (int i = 0; i < listTag.length; i++) {
            BerTlv berTlv = berTlvs.find(new BerTag(listTag[i]));
            if (berTlv != null)
                berTlvBuilder.addBerTlv(berTlv);
        }
        return berTlvBuilder.buildArray();
    }

    public EmvCardScheme getType() {
        return type;
    }

    public EmvTrack2 getTrack2() {
        return track2;
    }

    public EmvTrack1 getTrack1() {
        return track1;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public Date getCardExpireDate() {
        return cardExpireDate;
    }

    public int getCardSequenceNumber() {
        return cardSequenceNumber;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public String getAppLabel() {
        return appLabel;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public int getIssuerCountryCode() {
        return issuerCountryCode;
    }


    /**
     * Method used to find the real card scheme
     *
     * @param pAid        card complete AID
     * @param pCardNumber card number
     * @return card scheme
     */
    private EmvCardScheme findCardScheme(final String pAid, final String pCardNumber) {
        if (pAid == null) {
            type = EmvCardScheme.getCardTypeByCardNumber(pCardNumber);
        } else {
            type = EmvCardScheme.getCardTypeByAid(pAid);
            if (type == null) {
                type = EmvCardScheme.getCardTypeByCardNumber(pCardNumber);
            }
        }
        return type;
    }

    private static final int LEFT_MASK = 0xF0;

    private static final int RIGHT_MASK = 0xF;

    private static final char CHAR_SPACE = (char) 0x20;

    private static final int CHAR_DIGIT_ZERO = 0x30;

    private static final int CHAR_DIGIT_SEVEN = 0x37;

    private static final Pattern TRACK2_EQUIVALENT_PATTERN = Pattern.compile("([0-9]{1,19})D([0-9]{4})([0-9]{3})?(.*)");

    private static final Pattern TRACK1_PATTERN = Pattern.compile("%?([A-Z])([0-9]{1,19})(\\?[0-9])?\\^([^\\^]{2,26})" +
            "\\^([0-9]{4}|\\^)([0-9]{3}|\\^)([^\\?]+)\\??");

    /**
     * Extract track 1 data
     *
     * @param pRawTrack1 track1 raw data
     * @return EmvTrack1 object
     */
    public static EmvTrack1 extractTrack1Data(byte[] pRawTrack1) {
        EmvTrack1 ret = null;

        if (pRawTrack1 != null) {
            EmvTrack1 track1 = new EmvTrack1();
            track1.setRaw(pRawTrack1);
            Matcher m = TRACK1_PATTERN.matcher(new String(pRawTrack1));
            // Check pattern
            if (m.find()) {
                // Set format code
                track1.setFormatCode(m.group(1));
                // Set card number
                track1.setCardNumber(m.group(2));
                // Extract holder name
                String name = m.group(4).trim();
                if (name != null) {
                    track1.setCardHolderName(name);
                }
                // Read expire date
                SimpleDateFormat sdf = new SimpleDateFormat("yyMM", Locale.getDefault());
                try {
                    track1.setExpireDate(sdf.parse(m.group(5)));
                } catch (ParseException e) {
                    Log.e(null, "Unparsable expire card date : {}" + e.getMessage());
                    return ret;
                }
                // Read service
                track1.setService(m.group(6));
                ret = track1;
            }
        }
        return ret;
    }

    /**
     * Extract track 2 Equivalent data
     *
     * @param pRawTrack2 Raw track 2 data
     * @return EmvTrack2 object data or null
     */
    public static EmvTrack2 extractTrack2EquivalentData(byte[] pRawTrack2) {
        EmvTrack2 ret = null;

        if (pRawTrack2 != null) {
            EmvTrack2 track2 = new EmvTrack2();
            track2.setRaw(HexUtil.parseHex(HexUtil.toHexString(pRawTrack2).replace("D", "=")));
            String data = formatByte(pRawTrack2, false, false);
            Matcher m = TRACK2_EQUIVALENT_PATTERN.matcher(data);
            // Check pattern
            if (m.find()) {
                // read card number
                track2.setCardNumber(m.group(1));
                // Read expire date
                SimpleDateFormat sdf = new SimpleDateFormat("yyMM", Locale.getDefault());
                try {
                    track2.setExpireDate(sdf.parse(m.group(2)));
                } catch (ParseException e) {
                    Log.e(null, "Unparsable expire card date : {}" + e.getMessage());
                    return ret;
                }
                // Read service
                track2.setService(m.group(3));
                ret = track2;
            }
        }
        return ret;
    }

    private static String formatByte(final byte[] pByte, final boolean pSpace, final boolean pTruncate) {
        String result;
        if (pByte == null) {
            result = "";
        } else {
            int i = 0;
            if (pTruncate) {
                while (i < pByte.length && pByte[i] == 0) {
                    i++;
                }
            }
            if (i < pByte.length) {
                int sizeMultiplier = pSpace ? 3 : 2;
                char[] c = new char[(pByte.length - i) * sizeMultiplier];
                byte b;
                for (int j = 0; i < pByte.length; i++, j++) {
                    b = (byte) ((pByte[i] & LEFT_MASK) >> 4);
                    c[j] = (char) (b > 9 ? b + CHAR_DIGIT_SEVEN : b + CHAR_DIGIT_ZERO);
                    b = (byte) (pByte[i] & RIGHT_MASK);
                    c[++j] = (char) (b > 9 ? b + CHAR_DIGIT_SEVEN : b + CHAR_DIGIT_ZERO);
                    if (pSpace) {
                        c[++j] = CHAR_SPACE;
                    }
                }
                result = pSpace ? new String(c, 0, c.length - 1) : new String(c);
            } else {
                result = "";
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "EmvCard{" +
                "type=" + type +
                ", cardNumber='" + cardNumber + '\'' +
                ", cardExpireDate=" + cardExpireDate +
                ", cardSequenceNumber=" + cardSequenceNumber +
                ", cardHolderName='" + cardHolderName + '\'' +
                ", appLabel='" + appLabel + '\'' +
                ", serviceCode='" + serviceCode + '\'' +
                ", issuerCountryCode=" + issuerCountryCode +
                ", track2=" + track2 +
                ", track1=" + track1 +
                '}';
    }
}
