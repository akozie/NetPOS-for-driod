package com.woleapp.netpos.contactless.model

import android.os.Build
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.woleapp.netpos.contactless.nibss.NetPosTerminalConfig
import com.woleapp.netpos.contactless.util.Singletons

enum class MqttEvents(val event: String?) {
    AUTHENTICATION("AUTHENTICATION"),
    TERMINAL_CONFIGURATION("TERMINAL_CONFIGURATION"),
    TRANSACTIONS("TRANSACTION"),
    PRINTING_RECEIPT("PRINTING_RECEIPT"),
    NIP_PULL("BANK_TRANSFER"),
    NIP_NEW("GENERATE_SESSION_CODE"),
    NIP_SEARCH("VERIFY_SESSION_CODE"),
    CARD_READER_EVENTS("CARD_READER_EVENTS"),
    POWER_EVENTS("POWER_EVENTS"),
    BATTERY_EVENTS("BATTERY_EVENTS"),
    SMS_EVENTS("SMS_EVENTS")
}

enum class MqttTopics(val topic: String) {
    AUTHENTICATION("mqtt.pos.authentication.event"),
    TERMINAL_CONFIGURATION("mqtt.pos.terminal_config.event"),
    TRANSACTIONS("mqtt.pos.transaction.event"),
    PRINTING_RECEIPT("mqtt.pos.device.event"),
    NIP_PULL("mqtt.pos.bank_transfer.event"),
    NIP_NEW("mqtt.pos.generate_session_code.event"),
    NIP_SEARCH("mqtt.pos.verify_session_code.event"),
    CARD_READER_EVENTS("mqtt.pos.device.event"),
    POWER_EVENTS("mqtt.pos.device.event"),
    BATTERY_EVENTS("mqtt.pos.device.event"),
    SMS_EVENTS("mqtt.pos.sms.event")
}

enum class MqttStatus(val code: String) {
    SUCCESS("00"),
    ERROR("01")
}

@Entity(tableName = "mqttEvents")
data class MqttEventsLocal(
    val topic: String,
    val data: String,
    val cause: String? = null
){
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}

data class MqttEvent<T>(
    var storm_id: String? = null,
    var business_name: String? = null,
    var terminalId: String? = null,
    @SerializedName("serial_number") var deviceSerial: String? = null,
    @Ignore var data: T? = null,
    var event: String? = null,
    var status: String? = null,
    var code: String? = null,
    var timestamp: Long? = null,
    var geo: String? = null,
    var transactionType: String? = null
) {
    init {
        val user = Singletons.getCurrentlyLoggedInUser()
        storm_id = user!!.netplus_id!!
        business_name = user.business_name!!
        terminalId = NetPosTerminalConfig.getTerminalId()
        deviceSerial = Build.ID
    }
}

data class AuthenticationEventData(
    val business_name: String,
    val storm_id: String,
    @SerializedName("serial_number")val deviceSerial: String
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

data class SMSEvent(val to: String, val status: String, var serverResponse: String)

fun <T> MqttEvent<T>.toLocal(topic: String, cause: String? = null) = MqttEventsLocal(topic, Singletons.gson.toJson(this), cause)