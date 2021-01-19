package com.netpluspay.netpossdk.emv.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Objects;

public final class TransactionData implements Serializable {

    private String transId;

    private int cardType;

    private int transType;

    private int transState;

    private int appleVasState;

    private Double transAmount;

    private Double transCashAmount;

    private Calendar transDate;

    private byte[] transData;

    private byte[] appleVasData;

    public TransactionData() {
        this.transDate = Calendar.getInstance();
    }

    @NonNull
    public String getTransId() {
        return transId;
    }

    public void setTransId(@NonNull String transId) {
        this.transId = transId;
    }

    public int getCardType() {
        return cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }

    public int getTransType() {
        return transType;
    }

    public void setTransType(int transType) {
        this.transType = transType;
    }

    public Double getTransCashAmount() {
        return transCashAmount;
    }

    public void setTransCashAmount(Double transCashAmount) {
        this.transCashAmount = transCashAmount;
    }

    public int getTransState() {
        return transState;
    }

    public void setTransState(int transState) {
        this.transState = transState;
    }

    public int getAppleVasState() {
        return appleVasState;
    }

    public void setAppleVasState(int appleVasState) {
        this.appleVasState = appleVasState;
    }

    public Double getTransAmount() {
        return transAmount;
    }

    public void setTransAmount(Double transAmount) {
        this.transAmount = transAmount;
    }

    public Calendar getTransDate() {
        return transDate;
    }

    public void setTransDate(Calendar transDate) {
        this.transDate = transDate;
    }

    public byte[] getTransData() {
        return transData;
    }

    public void setTransData(byte[] transData) {
        this.transData = transData;
    }

    public byte[] getAppleVasData() {
        return appleVasData;
    }

    public void setAppleVasData(byte[] appleVasData) {
        this.appleVasData = appleVasData;
    }

    @Override
    public String toString() {
        return transId;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof TransactionData
                && this.transId.equals(((TransactionData) obj).transId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transId);
    }

    @Override
    protected Object clone() {
        return new TransactionData();
    }
}
