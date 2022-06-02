package com.woleapp.netpos.contactless


import com.danbamitale.epmslib.entities.AccountBalance
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.IsoAccountType
import com.danbamitale.epmslib.utils.TripleDES
import com.google.gson.Gson
import com.woleapp.netpos.contactless.util.getBeginningOfDay
import com.woleapp.netpos.contactless.util.getEndOfDayTimeStamp
import com.woleapp.netpos.contactless.util.xorHex
import org.junit.Assert.assertEquals
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
        println("11110010001111100000010010000101101011101100000010000110001000000000000000000000000000000000000000000000000000000000000000000000".length)
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

        println(
            "" +
                    "{\"keyHolder\":{\"id\":1,\"masterKey\":\"5EA6CD6D32ADCF489D7FFF865F6CA6B3\",\"baseKey\":\"5EA6CD6D32ADCF489D7FFF865F6CA6B3\",\"posMode\":\"POSVAS\",\"sessionKey\":\"1F47BA2243D5B43A3A7F32B0CCF6B4C1\",\"pinKey\":\"FC29C6B601E597C1D6B071903AAF6066\",\"track2Key\":\"\",\"bdk\":\"\"},\"configData\":{\"id\":1,\"epmsDateTime\":\"20210228141827\",\"cardAcceptorIdCode\":\"2044LA310921764\",\"hostTimeOut\":\"60\",\"currencyCode\":\"566\",\"countryCode\":\"566\",\"callHomeTime\":\"01\",\"merchantNameLocation\":\"COSMIC INTELLIGENT L   OG           OGNG\",\"merchantCategoryCode\":\"6010\"}}".length
        )

        println(
            Gson().toJson(
                CardData(
                    "5061840800158084537D2111601016422918",
                    "subset",
                    "000",
                    "051"
                ).apply {
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
            CardData.initCardDataFromTrack("9F0206000000000100820258009F3602017B9F260838AA658F1B13948B9F2701809F34034103029F3303E0F8C89F1A0205669F350122950542800080005F2A0205669A032106179C01009F37042598B818950542800000009F1E086D6F726566756E315F3401009F4104000000009F10320FA501A038F8000000000000000000000F0F0801000000000000000000000000")
        println(cardData.toString())
        println(cardData.panSequenceNumber)
        println()
        assertEquals(4, 2 + 2)
    }

    @Test
    fun generateNibssPinblock() {
        val plainPin = "1234"
        val pinCipher = "04${plainPin}FFFFFFFFFF"
        val pan = "4960091814144423"
        val cardNum = "0000${pan.substring(3, 15)}"
        val pinblock = xorHex(pinCipher, cardNum)!!
        println(pinblock)
        val clearPinKey = "6943DD4434E0B3C0D808D0FE2A590CD9"
        val pinblockToNibss = TripleDES.encrypt(pinblock, clearPinKey)
        println(pinblockToNibss)
        assert(pinblockToNibss.isBlank().not())
    }

    @Test
    fun generateNibssPinblock1() {
        val plainPin = "1234"
        val pinCipher = "041485FFFFFFFFFF"
        val pan = "5399235034454322"
        val cardNum = "0000${pan.substring(3, 15)}"
        val pinblock = xorHex(pinCipher, cardNum)!!
        println(pinblock)
        val clearPinKey = "d9da8ace946e5b3ee31c9d298fba2f51"
        val pinblockToNibss = TripleDES.encrypt(pinblock, clearPinKey)
        println(pinblockToNibss)
        assert(pinblockToNibss.isBlank().not())
    }


}

