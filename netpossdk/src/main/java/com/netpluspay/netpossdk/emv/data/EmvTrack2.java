package com.netpluspay.netpossdk.emv.data;


import com.netpluspay.netpossdk.utils.tlv.HexUtil;

import java.util.Date;

public class EmvTrack2 {

    /**
     * Raw track 2 data
     */
    private byte[] raw;

    /**
     * Card number
     */
    private String cardNumber;

    /**
     * Expiration date
     */
    private Date expireDate;

    /**
     * Card services
     */
    private String service;

    /**
     * @return the raw
     */
    public byte[] getRaw() {
        return raw;
    }

    /**
     * @param raw the raw to set
     */
    public void setRaw(final byte[] raw) {
        this.raw = raw;
    }

    /**
     * @return the cardNumber
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * @param cardNumber the cardNumber to set
     */
    public void setCardNumber(final String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * @return the expireDate
     */
    public Date getExpireDate() {
        return expireDate;
    }

    /**
     * @param expireDate the expireDate to set
     */
    public void setExpireDate(final Date expireDate) {
        this.expireDate = expireDate;
    }

    /**
     * @return the service
     */
    public String getService() {
        return service;
    }

    /**
     * @param service the service to set
     */
    public void setService(final String service) {
        this.service = service;
    }

    @Override
    public String toString() {
        return "EmvTrack2{" +
                "raw=" + HexUtil.toHexString(raw) +
                ", cardNumber='" + cardNumber + '\'' +
                ", expireDate=" + expireDate +
                ", service='" + service + '\'' +
                '}';
    }
}
