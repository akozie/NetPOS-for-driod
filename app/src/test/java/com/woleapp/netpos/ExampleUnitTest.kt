package com.woleapp.netpos

import com.danbamitale.epmslib.entities.AccountBalance
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.IsoAccountType
import com.danbamitale.epmslib.utils.TripleDES
import com.google.gson.Gson
import com.woleapp.netpos.util.getBeginningOfDay
import com.woleapp.netpos.util.getEndOfDayTimeStamp
import org.junit.Assert.*
import org.junit.Test
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
        val s =
            "{\"business_name\":\"CamelCase\",\"code\":\"00\",\"data\":{\"AID\":\"\",\"RRN\":\"201216183858\",\"STAN\":\"183858\",\"TSI\":\"\",\"TVR\":\"\",\"accountType\":\"SAVINGS\",\"acquiringInstCode\":\"539941\",\"additionalAmount\":0,\"amount\":200,\"appCryptogram\":\"\",\"authCode\":\"iN2fgd\",\"cardExpiry\":\"2409\",\"cardHolder\":\"\",\"cardLabel\":\"\",\"id\":0,\"localDate\":\"1216\",\"localTime\":\"183858\",\"maskedPan\":\"539941xxxxxx4402\",\"merchantId\":\"2057LA100007032\",\"originalForwardingInstCode\":\"627629\",\"otherAmount\":0,\"otherId\":\"\",\"responseCode\":\"00\",\"responseDE55\":\"910ABCB033ACEA86509A0010\",\"responseMessage\":\"Approved\",\"terminalId\":\"2057H63U\",\"transactionTimeInMillis\":1608140338299,\"transactionType\":\"PURCHASE\",\"transmissionDateTime\":\"1216183858\"},\"deviceSerial\":\"B1791E1XL8080026\",\"event\":\"TRANSACTION\",\"geo\":\"lat:7.390478333333334 long:3.8802483333333337\",\"status\":\"Approved\",\"storm_id\":\"66f05f15-a3f6-4654-9aa9-3eea3a3360e1\",\"terminalId\":\"2057H63U\",\"timestamp\":1608140339954,\"transactionType\":\"PURCHASE\"}"
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTypeConverterFunction() {
        val sampleType = TransactionType.PURCHASE
        val sampleName = sampleType.name
//        println(sampleName)
//        println(TransactionType.valueOf(sampleName).name)

        println("" +
                "{\"keyHolder\":{\"id\":1,\"masterKey\":\"5EA6CD6D32ADCF489D7FFF865F6CA6B3\",\"baseKey\":\"5EA6CD6D32ADCF489D7FFF865F6CA6B3\",\"posMode\":\"POSVAS\",\"sessionKey\":\"1F47BA2243D5B43A3A7F32B0CCF6B4C1\",\"pinKey\":\"FC29C6B601E597C1D6B071903AAF6066\",\"track2Key\":\"\",\"bdk\":\"\"},\"configData\":{\"id\":1,\"epmsDateTime\":\"20210228141827\",\"cardAcceptorIdCode\":\"2044LA310921764\",\"hostTimeOut\":\"60\",\"currencyCode\":\"566\",\"countryCode\":\"566\",\"callHomeTime\":\"01\",\"merchantNameLocation\":\"COSMIC INTELLIGENT L   OG           OGNG\",\"merchantCategoryCode\":\"6010\"}}".length)

        println(
            Gson().toJson(CardData("5061840800158084537D2111601016422918", "subset", "000", "051").apply {
                pinBlock = "a2abcdef93901"
            })
        )
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTripleDesEncrypt() {
        val pib = TripleDES.encrypt("0420BDCBA669F8F9", "ab20ef34b9ea1c9425132c0816c22951")
        println(pib)
        assertEquals("1631f8f70ecad7c3", pib)
    }

    @Test
    fun getSampleNibssSubset() {
        val balance = "0001566C0000001138000002566C000000113800"
        println(balance.length)
        parseField54AdditionalAmount(balance).forEach { accountBalance ->
            println(
                "account: " + accountBalance.accountType.name + " balance: " + accountBalance.amount / 100 + " amountSign: ${accountBalance.amountSign} currencyCode: ${accountBalance.currencyCode} amountType: ${accountBalance.amountType}"
            )
        }
        assertEquals(4, 2 + 2)
    }

    private fun parseAdditionalAmountString(inputString: String): AccountBalance {
        if (inputString.length < 20) error("Invalid string")

        val accountType = IsoAccountType.parseIntAccountType(inputString.substring(0, 2).toInt())
        val amountType = inputString.substring(2, 4);
        val currencyCode = inputString.substring(4, 7)
        val amountSign = inputString[7]
        val amount = inputString.substring(8, 20).toLong()

        return AccountBalance(accountType, amountType, currencyCode, amountSign, amount)
    }

    fun parseField54AdditionalAmount(inputString: String): List<AccountBalance> {
        if (inputString.length < 20) return listOf()

        val list = ArrayList<AccountBalance>()
        var count = 0;
        do {
            list.add(parseAdditionalAmountString(inputString.substring(count, count + 20)))
            count += 20;
        } while (count + 20 <= inputString.length)

        return list
    }

    @Test
    fun check_if_icc_correct() {
        val cardData =
            CardData.initCardDataFromTrack("820238009F360200939F2701809F34034103029F1E086D6F726566756E319F100706011203A4B0109F3303E0F8C89F3501229F37045FD9345A9F01063132333435369F03060000000000008104000000C89F02060000000002005F24032311305F25032010205A0848484211638175015F3401019F150230319F160F3030303030303030303030303030309F1A0205669F1C08313233313233313257104848421163817501D2311226180133195F2A0205669F21031541179C01008E1800000000000000004105440502054103440342035E031F029F0D0598409C98009F0E0500100000009F4005FF80F000019F2608A924F203659E06759F0702FF809A032102225F280205669F090200009F4104000000009F0F0598409C98005F201A434F534D494320494E54454C4C4947454E542F504F5320544541950508800000009B02E8009F0607A0000000031010500C5669736120507265706169648407A0000000031010")
        println(cardData.toString())
        println(cardData.pan)
        println()
        assertEquals(4, 2 + 2)
    }
}

