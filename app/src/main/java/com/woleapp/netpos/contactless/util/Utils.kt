package com.woleapp.netpos.contactless.util


import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import timber.log.Timber
import kotlin.experimental.and


fun dumpTagData(tag: Tag): String {
    Timber.e("dump tag")
    val sb = StringBuilder()
    val id: ByteArray = tag.id
    sb.append("ID (hex): ").append(toHex(id)).append('\n')
    sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n')
    sb.append("ID (dec): ").append(toDec(id)).append('\n')
    sb.append("ID (reversed dec): ").append(toReversedDec(id)).append('\n')
    val prefix = "android.nfc.tech."
    sb.append("Technologies: ")
    for (tech in tag.techList) {
        sb.append(tech.substring(prefix.length))
        sb.append(", ")
    }
    sb.delete(sb.length - 2, sb.length)
    for (tech in tag.techList) {
        if (tech == MifareClassic::class.java.name) {
            sb.append('\n')
            var type = "Unknown"
            try {
                val mifareTag = MifareClassic.get(tag)
                when (mifareTag.type) {
                    MifareClassic.TYPE_CLASSIC -> type = "Classic"
                    MifareClassic.TYPE_PLUS -> type = "Plus"
                    MifareClassic.TYPE_PRO -> type = "Pro"
                }
                sb.append("Mifare Classic type: ")
                sb.append(type)
                sb.append('\n')
                sb.append("Mifare size: ")
                sb.append(mifareTag.size.toString() + " bytes")
                sb.append('\n')
                sb.append("Mifare sectors: ")
                sb.append(mifareTag.sectorCount)
                sb.append('\n')
                sb.append("Mifare blocks: ")
                sb.append(mifareTag.blockCount)
            } catch (e: Exception) {
                sb.append("Mifare classic error: " + e.message)
            }
        }
        if (tech == MifareUltralight::class.java.name) {
            sb.append('\n')
            val mifareUlTag = MifareUltralight.get(tag)
            var type = "Unknown"
            when (mifareUlTag.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> type = "Ultralight"
                MifareUltralight.TYPE_ULTRALIGHT_C -> type = "Ultralight C"
            }
            sb.append("Mifare Ultralight type: ")
            sb.append(type)
        }
    }
    return sb.toString()
}

private fun toHex(bytes: ByteArray): String {
    val sb = java.lang.StringBuilder()
    for (i in bytes.indices.reversed()) {
        val b = bytes[i] and (0xff).toByte()
        if (b < 0x10) sb.append('0')
        sb.append(Integer.toHexString(b.toInt()))
        if (i > 0) {
            sb.append(" ")
        }
    }
    return sb.toString()
}

private fun toReversedHex(bytes: ByteArray): String {
    val sb = java.lang.StringBuilder()
    for (i in bytes.indices) {
        if (i > 0) {
            sb.append(" ")
        }
        val b = bytes[i] and (0xff).toByte()
        if (b < 0x10) sb.append('0')
        sb.append(Integer.toHexString(b.toInt()))
    }
    return sb.toString()
}

private fun toDec(bytes: ByteArray): Long {
    var result: Long = 0
    var factor: Long = 1
    for (i in bytes.indices) {
        val value = bytes[i] and (0xffL).toByte()
        result += value * factor
        factor *= 256L
    }
    return result
}

private fun toReversedDec(bytes: ByteArray): Long {
    var result: Long = 0
    var factor: Long = 1
    for (i in bytes.indices.reversed()) {
        val value = bytes[i] and (0xffL).toByte()
        result += value * factor
        factor *= 256L
    }
    return result
}

val NFC_INTENT_FILTER = arrayOf(
    IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
    IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
)
val TECH_LIST = arrayOf(
    arrayOf(
        NfcA::class.java.name, IsoDep::class.java.name
    )
)

const val NFC_A_TAG = "TAG: Tech [android.nfc.tech.IsoDep, android.nfc.tech.NfcA]"
const val NFC_B_TAG = "TAG: Tech [android.nfc.tech.IsoDep, android.nfc.tech.NfcB]"