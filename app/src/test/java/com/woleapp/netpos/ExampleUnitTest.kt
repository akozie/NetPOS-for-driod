package com.woleapp.netpos

import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.TripleDES
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
}
