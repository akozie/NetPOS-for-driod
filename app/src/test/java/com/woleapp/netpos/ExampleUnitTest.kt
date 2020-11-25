package com.woleapp.netpos

import com.danbamitale.epmslib.entities.TransactionType
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
}
