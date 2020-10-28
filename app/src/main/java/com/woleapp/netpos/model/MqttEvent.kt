package com.woleapp.netpos.model

enum class MqttEvents(val event: String?) {
    AUTHENTICATION("AUTHENTICATION"),
    TERMINAL_CONFIGURATION("TERMINAL_CONFIGURATION"),
    TRANSACTIONS("TRANSACTIONS"),
    PRINTING_RECEIPT("PRINTING_RECEIPT"),
    NIP_PULL("BANK_TRANSFER"),
    NIP_NEW("GENERATE_SESSION_CODE"),
    NIP_SEARCH("VERIFY_SESSION_CODE"),
    CARD_READER_EVENTS("CARD_READER_EVENTS"),
    POWER_EVENTS("POWER_EVENTS"),
    BATTERY_EVENTS("BATTERY_EVENTS")

}

enum class MqttStatus(val code: String) {
    SUCCESS("00"),
    ERROR("01")
}

data class MqttEvent(
    val storm_id: String,
    val business_name: String,
    var terminalId: String,
    val deviceSerial: String,
    var data: Any? = null,
    var event: String? = null,
    var status: String? = null,
    var code: String? = null,
    var timestamp: Long? = null,
    var geo: String? = null,
    var transactionType: String? = null
)

data class AuthenticationEventData(
    val business_name: String,
    val storm_id: String,
    val deviceSerial: String
)

data class PrinterEventData(val transactionRef: String, val printerCode: String)

data class NipEvent(
    val session_code: String? = null,
    val code_verified: Boolean? = null,
    val start_date: String? = null,
    val end_date: String? = null
)

data class BatteryEvents(val battery_percentage: Int, val status: String)

data class CardReaderMqttEvent(
    val cardExpiry: String? = null,
    val cardHolder: String? = null,
    val maskedPan: String? = null,
    val readerError: String? = null
)