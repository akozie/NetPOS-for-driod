package com.woleapp.netpos

import com.danbamitale.epmslib.entities.TransactionType
import com.socsi.utils.HexDump
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
        println(HexDump.hexStr2Str("4A2F41444547424F5945"))
        println(HexDump.toHexString(java.lang.Byte.parseByte("${16/2}")))
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTypeConverterFunction(){
        val sampleType = TransactionType.PURCHASE
        val sampleName = sampleType.name
        println(sampleName)
        println(TransactionType.valueOf(sampleName).name)
        assertEquals(4, 2+2)
    }
}
