package com.woleapp.netpos.taponphone.visa

import java.util.*

val supportedAids = ArrayList(
    listOf(
        "A0000000031010",
        "A0000000041010"
    )
)

val REQUIRED_TAGS = arrayOf(
    "9F26",
    "9F27",
    "9F10",
    "9F37",
    "9F36",
    "95",
    "9A",
    "9C",
    "9F02",
    "5F2A",
    "82",
    "9F1A",
    "9F34",
    "9F33",
    "9F35",
    "9F1E",
    "84",
    "9F09",
    "9F03",
    "5F34",
    "8E",
    "57"
)

enum class NfcPaymentType {
    VISA, MASTERCARD
}