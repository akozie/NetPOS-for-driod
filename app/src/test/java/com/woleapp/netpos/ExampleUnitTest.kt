package com.woleapp.netpos

import com.danbamitale.epmslib.entities.TransactionType
import org.apache.commons.lang.StringUtils
import org.junit.Test

import org.junit.Assert.*

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
        println("519911xxxxxx1994")
        println(StringUtils.overlay("5199118765421994", "xxxxxx", 6, 12))
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
