package com.netpluspay.netpossdk.emv.data;


import com.netpluspay.netpossdk.utils.tlv.HexUtil;

import java.util.Date;

public class EmvTrack1 {
	/**
	 * Raw track 1 data
	 */
	private byte[] raw;

	/**
	 * Format code
	 */
	private String formatCode;

	/**
	 * Card number
	 */
	private String cardNumber;

	/**
	 * Expiration date
	 */
	private Date expireDate;

	/**
	 * Card HolderName
	 */
	private String cardHolderName;

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
	 * @param raw
	 *            the raw to set
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
	 * @param cardNumber
	 *            the cardNumber to set
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
	 * @param expireDate
	 *            the expireDate to set
	 */
	public void setExpireDate(final Date expireDate) {
		this.expireDate = expireDate;
	}

	/**
	 * @return the formatCode
	 */
	public String getFormatCode() {
		return formatCode;
	}

	/**
	 * @param formatCode
	 *            the formatCode to set
	 */
	public void setFormatCode(final String formatCode) {
		this.formatCode = formatCode;
	}

	/**
	 * @return the cardHolderName
	 */
	public String getCardHolderName() {
		return cardHolderName;
	}

	/**
	 * @param cardHolderName
	 *            the cardHolderName to set
	 */
	public void setCardHolderName(final String cardHolderName) {
		this.cardHolderName = cardHolderName;
	}

	/**
	 * @return the service
	 */
	public String getService() {
		return service;
	}

	/**
	 * @param service
	 *            the service to set
	 */
	public void setService(final String service) {
		this.service = service;
	}

	@Override
	public String toString() {
		return "EmvTrack1{" +
				"raw=" + HexUtil.toHexString(raw) +
				", formatCode='" + formatCode + '\'' +
				", cardNumber='" + cardNumber + '\'' +
				", expireDate=" + expireDate +
				", cardHolderName='" + cardHolderName + '\'' +
				", service='" + service + '\'' +
				'}';
	}
}
