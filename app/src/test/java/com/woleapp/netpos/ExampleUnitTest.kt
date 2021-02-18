package com.woleapp.netpos

import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.TripleDES
import com.netpluspay.netpossdk.utils.HexDump
import com.woleapp.netpos.util.getBeginningOfDay
import com.woleapp.netpos.util.getEndOfDayTimeStamp
import com.woleapp.netpos.util.playAround
import org.apache.commons.lang.StringUtils
import org.junit.Test

import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        //println(HexDump.hexStr2Str("4A2F41444547424F5945"))
        //println(HexDump.toHexString(java.lang.Byte.parseByte("${16/2}")))
        //println("519911xxxxxx1994")
        //println(StringUtils.overlay("5199118765421994", "xxxxxx", 6, 12))
        //println(SimpleDateFormat("dd/MM/YYYY HH:mm a").format(Date(1601723293000)))
        //println(playAround())
        //println("78b4311d3e83a85c924edcc60dddc99d76fde9a21459e879fcd88878a31de8c5" == "78b4311d3e83a85c924edcc60dddc99d76fde9a21459e879fcd88878a31de8c5")
        println(SimpleDateFormat("y-MM-dd HH:mm:ss").format(getBeginningOfDay(1606290839000)))
        println(SimpleDateFormat("y-MM-dd HH:mm:ss").format(getEndOfDayTimeStamp(1606290839000)))
        val s = "{\"business_name\":\"CamelCase\",\"code\":\"00\",\"data\":{\"AID\":\"\",\"RRN\":\"201216183858\",\"STAN\":\"183858\",\"TSI\":\"\",\"TVR\":\"\",\"accountType\":\"SAVINGS\",\"acquiringInstCode\":\"539941\",\"additionalAmount\":0,\"amount\":200,\"appCryptogram\":\"\",\"authCode\":\"iN2fgd\",\"cardExpiry\":\"2409\",\"cardHolder\":\"\",\"cardLabel\":\"\",\"id\":0,\"localDate\":\"1216\",\"localTime\":\"183858\",\"maskedPan\":\"539941xxxxxx4402\",\"merchantId\":\"2057LA100007032\",\"originalForwardingInstCode\":\"627629\",\"otherAmount\":0,\"otherId\":\"\",\"responseCode\":\"00\",\"responseDE55\":\"910ABCB033ACEA86509A0010\",\"responseMessage\":\"Approved\",\"terminalId\":\"2057H63U\",\"transactionTimeInMillis\":1608140338299,\"transactionType\":\"PURCHASE\",\"transmissionDateTime\":\"1216183858\"},\"deviceSerial\":\"B1791E1XL8080026\",\"event\":\"TRANSACTION\",\"geo\":\"lat:7.390478333333334 long:3.8802483333333337\",\"status\":\"Approved\",\"storm_id\":\"66f05f15-a3f6-4654-9aa9-3eea3a3360e1\",\"terminalId\":\"2057H63U\",\"timestamp\":1608140339954,\"transactionType\":\"PURCHASE\"}"
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTypeConverterFunction() {
        val sampleType = TransactionType.PURCHASE
        val sampleName = sampleType.name
        println(sampleName)
        println(TransactionType.valueOf(sampleName).name)
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTripleDesEncrypt() {
        val pib = TripleDES.encrypt("0420BDCBA669F8F9", "ab20ef34b9ea1c9425132c0816c22951")
        println(pib)
        assertEquals("1631f8f70ecad7c3", pib)
    }

    @Test
    fun getSampleNibssSubset(){
        val builder = java.lang.StringBuilder()
        builder.append("9F26")
            .append(HexDump.toHexString(("${"B6857B911357BE14".length / 2}").toByte()))
            .append(
                "B6857B911357BE14"
            )
            .append("9F27").append(HexDump.toHexString(("${"80".length / 2}").toByte()))
            .append(
                "80"
            )
            .append("9F10")
            .append(HexDump.toHexString(("${"0FA501A039F80400000000000000000000000000000000000000000000000000".length / 2}").toByte()))
            .append(
                "0FA501A039F80400000000000000000000000000000000000000000000000000"
            )
            .append("9F37")
            .append(HexDump.toHexString(("${"A7CACA48".length / 2}").toByte()))
            .append(
                "A7CACA48"
            )
            .append("9F36")
            .append(HexDump.toHexString(("${"0030".length / 2}").toByte()))
            .append(
                "0030"
            )
            .append("95")
            .append(HexDump.toHexString(("${"0000008000".length / 2}").toByte()))
            .append(
                "0000008000"
            )
            .append("9A")
            .append(HexDump.toHexString(("${"210126".length / 2}").toByte()))
            .append(
                "210126"
            )
            .append("9C")
            .append(HexDump.toHexString(("${"00".length / 2}").toByte()))
            .append(
                "00"
            )
            .append("9F02").append(HexDump.toHexString(("${"000000000200".length / 2}").toByte()))
            .append("000000000200")
            .append("5F2A")
            .append(HexDump.toHexString(("${"0566".length / 2}").toByte()))
            .append(
                "0566"
            )
            .append("82")
            .append(HexDump.toHexString(("${"3800".length / 2}").toByte()))
            .append(
                "3800"
            )
            .append("9F1A")
            .append(HexDump.toHexString(("${"0566".length / 2}").toByte()))
            .append(
                "0566"
            )
            .append("9F34")
            .append(HexDump.toHexString(("${"440302".length / 2}").toByte()))
            .append(
                "440302"
            )
            .append("9F33")
            .append(HexDump.toHexString(("${"E0F8C8".length / 2}").toByte()))
            .append(
                "E0F8C8"
            )
            .append("9F35")
            .append(HexDump.toHexString(("${"22".length / 2}").toByte()))
            .append(
                "22"
            )
            .append("9F1E")
            .append(HexDump.toHexString(("${"B 1 7 9 1 E 1 X".length / 2}").toByte()))
            .append(
                "B 1 7 9 1 E 1 X"
            )
            .append("84")
            .append(HexDump.toHexString(("${"A0000000041010".length / 2}").toByte()))
            .append(
                "A0000000041010"
            )
            .append("9F09")
            .append(HexDump.toHexString(("${"0002".length / 2}").toByte()))
            .append(
                "0002"
            )
            .append("9F03")
            .append(HexDump.toHexString(("${"000000000000".length / 2}").toByte()))
            .append(
                "000000000000"
            )
            .append("5F34")
            .append(HexDump.toHexString(("${"01".length / 2}").toByte()))
            .append(
                "01"
            )
        //.append("91").append(this.issueAuthenticationData);
        //.append("9F6E").append(this.formFactorIndicator);
        println(builder.toString())
        assertEquals(4, 2 + 2)
    }
}
