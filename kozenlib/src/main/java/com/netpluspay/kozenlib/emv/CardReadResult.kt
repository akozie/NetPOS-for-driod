package com.netpluspay.kozenlib.emv

import android.util.Log
import com.netpluspay.kozenlib.emv.data.TransactionData
import com.netpluspay.kozenlib.utils.tlv.BerTlvParser
import com.netpluspay.kozenlib.utils.tlv.HexUtil
import org.apache.commons.lang.StringUtils

class CardReadResult(val readResultCode: Int, transactionData: TransactionData) {
    var applicationPrimaryAccountNumber: String? = null
        private set
    var cardHolderName: String? = null
        private set
    var track2Data: String? = null
        private set
    var applicationPANSequenceNumber: String? = null
        private set
    var cryptogram: String? = null
        private set
    val issuerApplicationData: String? = null
    var unpredictableNumber: String? = null
        private set
    var applicationTransactionCounter: String? = null
        private set
    var terminalVerificationResults: String? = null
        private set
    var transactionDate: String? = null
        private set
    var transactionType: String? = null
        private set
    var amount: String? = null
        private set
    var transactionCurrencyCode: String? = null
        private set
    var applicationInterchangeProfile: String? = null
        private set
    var terminalCountryCode: String? = null
        private set
    var cashBackAmount: String? = null
        private set
    var terminalCapabilities: String? = null
        private set
    var cardholderVerificationMethod: String? = null
        private set
    var terminalType: String? = null
        private set
    var deviceSerialNumber: String? = null
        private set
    var authorizationResponseCode: String? = null
        private set
    var applicationVersionNumber: String? = null
        private set
    var transactionSequenceNumber: String? = null
        private set
    var expirationDate: String? = null
        private set
    val iCCRelateData: String? = null
    private var authorizationRequest: String? = null
    private var issuerApplicationDiscretionaryData: String? = null
    private var formFactorIndicator: String? = null
    private var originalPan: String? = null
    var encryptedPinBlock: String? = null
    private fun extractValues(transactionData: TransactionData) {
        val mTlvParser = BerTlvParser()
        val data = transactionData.transData
        val mTlvs = mTlvParser.parse(data)
        val sp = StringBuffer()
        for (tlv in mTlvs.list) {
            when (tlv.tag.berTagHex) {
                "5A" -> applicationPrimaryAccountNumber = tlv.hexValue
                "5F20" -> cardHolderName = tlv.textValue
                "57" -> track2Data = tlv.hexValue
                "5F34" -> {
                    originalPan = tlv.hexValue
                    applicationPANSequenceNumber = StringUtils.leftPad(tlv.hexValue, 3, '0')
                }
                "9F27" -> cryptogram = tlv.hexValue
                "9F37" -> unpredictableNumber = tlv.hexValue
                "9F36" -> applicationTransactionCounter = tlv.hexValue
                "95" -> terminalVerificationResults = tlv.hexValue
                "9A" -> transactionDate = tlv.hexValue
                "9C" -> transactionType = tlv.hexValue
                "9F02" -> amount = tlv.hexValue
                "5F24" -> expirationDate = tlv.hexValue
                "5F2A" -> {
                    transactionCurrencyCode = tlv.hexValue
                    applicationInterchangeProfile = tlv.hexValue
                }
                "82" -> applicationInterchangeProfile = tlv.hexValue
                "9F1A" -> terminalCountryCode = tlv.hexValue
                "9F03" -> cashBackAmount = tlv.hexValue
                "9F33" -> terminalCapabilities = tlv.hexValue
                "9F34" -> cardholderVerificationMethod = tlv.hexValue
                "9F35" -> terminalType = tlv.hexValue
                "9F1E" -> deviceSerialNumber = tlv.hexValue
                "84" -> authorizationResponseCode = tlv.hexValue
                "9F09" -> applicationVersionNumber = tlv.hexValue
                "9F41" -> transactionSequenceNumber = tlv.hexValue
                "9F26" -> authorizationRequest = tlv.hexValue
                "9F10" -> issuerApplicationDiscretionaryData = tlv.hexValue
                "9F6E" -> formFactorIndicator = tlv.hexValue
            }
        }
        Log.e("TAG", "Extract values")
    }

    //.append("91").append(this.issueAuthenticationData);
    //.append("9F6E").append(this.formFactorIndicator);
    val nibssIccSubset: String
        get() {
            val builder = StringBuilder()
            builder.append("9F26")
                .append(HexUtil.toHexString(("" + authorizationRequest!!.length / 2).encodeToByteArray()))
                .append(
                    authorizationRequest
                )
                .append("9F27")
                .append(HexUtil.toHexString(("" + cryptogram!!.length / 2).encodeToByteArray())).append(
                    cryptogram
                )
                .append("9F10")
                .append(HexUtil.toHexString(("" + issuerApplicationDiscretionaryData!!.length / 2).encodeToByteArray()))
                .append(
                    issuerApplicationDiscretionaryData
                )
                .append("9F37")
                .append(HexUtil.toHexString(("" + unpredictableNumber!!.length / 2).encodeToByteArray()))
                .append(
                    unpredictableNumber
                )
                .append("9F36")
                .append(HexUtil.toHexString(("" + applicationTransactionCounter!!.length / 2).encodeToByteArray()))
                .append(
                    applicationTransactionCounter
                )
                .append("95")
                .append(HexUtil.toHexString(("" + terminalVerificationResults!!.length / 2).encodeToByteArray()))
                .append(
                    terminalVerificationResults
                )
                .append("9A")
                .append(HexUtil.toHexString(("" + transactionDate!!.length / 2).encodeToByteArray()))
                .append(
                    transactionDate
                )
                .append("9C")
                .append(HexUtil.toHexString(("" + transactionType!!.length / 2).encodeToByteArray()))
                .append(
                    transactionType
                )
                .append("9F02")
                .append(HexUtil.toHexString(("" + amount!!.length / 2).encodeToByteArray())).append(
                    amount
                )
                .append("5F2A")
                .append(HexUtil.toHexString(("" + transactionCurrencyCode!!.length / 2).encodeToByteArray()))
                .append(
                    transactionCurrencyCode
                )
                .append("82")
                .append(HexUtil.toHexString(("" + applicationInterchangeProfile!!.length / 2).encodeToByteArray()))
                .append(
                    applicationInterchangeProfile
                )
                .append("9F1A")
                .append(HexUtil.toHexString(("" + terminalCountryCode!!.length / 2).encodeToByteArray()))
                .append(
                    terminalCountryCode
                )
                .append("9F34")
                .append(HexUtil.toHexString(("" + cardholderVerificationMethod!!.length / 2).encodeToByteArray()))
                .append(
                    cardholderVerificationMethod
                )
                .append("9F33")
                .append(HexUtil.toHexString(("" + terminalCapabilities!!.length / 2).encodeToByteArray()))
                .append(
                    terminalCapabilities
                )
                .append("9F35")
                .append(HexUtil.toHexString(("" + terminalType!!.length / 2).encodeToByteArray())).append(
                    terminalType
                )
                .append("9F1E")
                .append(HexUtil.toHexString(("" + deviceSerialNumber!!.length / 2).encodeToByteArray()))
                .append(
                    deviceSerialNumber
                )
                .append("84")
                .append(HexUtil.toHexString(("" + authorizationResponseCode!!.length / 2).encodeToByteArray()))
                .append(
                    authorizationResponseCode
                )
                .append("9F09")
                .append(HexUtil.toHexString(("" + applicationVersionNumber!!.length / 2).encodeToByteArray()))
                .append(
                    applicationVersionNumber
                )
                .append("9F03")
                .append(HexUtil.toHexString(("" + cashBackAmount!!.length / 2).encodeToByteArray()))
                .append(
                    cashBackAmount
                )
                .append("5F34")
                .append(HexUtil.toHexString(("" + originalPan!!.length / 2).encodeToByteArray())).append(
                    originalPan
                )
            //.append("91").append(this.issueAuthenticationData);
            //.append("9F6E").append(this.formFactorIndicator);
            return builder.toString()
        }

    override fun toString(): String {
        return "CardReadResult{" +
                "cardResultCode=" + readResultCode +
                ", applicationPrimaryAccountNumber='" + applicationPrimaryAccountNumber + '\'' +
                ", cardHolderName='" + cardHolderName + '\'' +
                ", track2Data='" + track2Data + '\'' +
                ", applicationPANSequenceNumber='" + applicationPANSequenceNumber + '\'' +
                ", cryptogram='" + cryptogram + '\'' +
                ", issuerApplicationData='" + issuerApplicationData + '\'' +
                ", unpredictableNumber='" + unpredictableNumber + '\'' +
                ", applicationTransactionCounter='" + applicationTransactionCounter + '\'' +
                ", terminalVerificationResults='" + terminalVerificationResults + '\'' +
                ", transactionDate='" + transactionDate + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", amount='" + amount + '\'' +
                ", transactionCurrencyCode='" + transactionCurrencyCode + '\'' +
                ", applicationInterchangeProfile='" + applicationInterchangeProfile + '\'' +
                ", terminalCountryCode='" + terminalCountryCode + '\'' +
                ", cashBackAmount='" + cashBackAmount + '\'' +
                ", terminalCapabilities='" + terminalCapabilities + '\'' +
                ", cardholderVerificationMethod='" + cardholderVerificationMethod + '\'' +
                ", terminalType='" + terminalType + '\'' +
                ", deviceSerialNumber='" + deviceSerialNumber + '\'' +
                ", authorizationResponseCode='" + authorizationResponseCode + '\'' +
                ", applicationVersionNumber='" + applicationVersionNumber + '\'' +
                ", transactionSequenceNumber='" + transactionSequenceNumber + '\'' +
                ", authorizationRequest='" + authorizationRequest + '\'' +
                ", issuerApplicationDiscretionaryData='" + issuerApplicationDiscretionaryData + '\'' +
                ", expirationDate='" + expirationDate + '\'' +
                '}'
    }

    //return cardResultCode == EmvApi.EMV_TRANS_ACCEPT || cardResultCode == EmvApi.EMV_TRANS_QPBOC_ACCEPT;
    val isEMVAccepted: Boolean
        get() = true

    //return cardResultCode == EmvApi.EMV_TRANS_ACCEPT || cardResultCode == EmvApi.EMV_TRANS_QPBOC_ACCEPT;
    init {
        extractValues(transactionData)
    }
}