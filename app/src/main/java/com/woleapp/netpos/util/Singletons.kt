package com.woleapp.netpos.util

import com.google.gson.Gson
import com.netpluspay.nibssclient.models.KeyHolder
import com.netpluspay.nibssclient.models.TransactionResponse
import com.netpluspay.nibssclient.models.responseMessage
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.ConfigurationData
import com.woleapp.netpos.model.NibssResponse
import com.woleapp.netpos.model.User
import com.woleapp.netpos.nibss.DEFAULT_TERMINAL_ID
import java.lang.Exception

fun useStormTerminalId() = Prefs.getBoolean(PREF_USE_STORM_TERMINAL_ID, true)
fun TransactionResponse.toNibssResponse(remark: String? = null): NibssResponse =
    Singletons.gson.fromJson(
        Singletons.gson.toJson(this), NibssResponse::class.java
    ).also {
        it.responseMessage = try {
            this.responseMessage
        } catch (e: Exception) {
            ""
        }
        it.additionalAmount = this.additionalAmount_54.toLongOrNull() ?: 0
        it.localDate = this.localDate_13
        it.localTime = this.localTime_12
        it.amount = this.amount.div(100)
        remark?.let { r ->
            it.remark = r
        }
    }

object Singletons {
    fun setUseStormTid(useStormTid: Boolean) =
        Prefs.putBoolean(PREF_USE_STORM_TERMINAL_ID, useStormTid)

    val gson = Gson()
    fun getCurrentlyLoggedInUser(): User? =
        gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)

    fun getSavedConfigurationData(): ConfigurationData {
        val configurationData: ConfigurationData? =
            gson.fromJson(
                Prefs.getString(PREF_CONFIGURATION_DATA, null),
                ConfigurationData::class.java
            )

        return configurationData ?: ConfigurationData(
            "DEFAULT_NIBSS_IP",
            "DEFAULT_NIBSS_PORT",
            DEFAULT_TERMINAL_ID,
            "Keys.liveKey1",
            "Keys.liveKey2"
        )
//        configurationData?.let {
//            return it
//        }
//        return ConfigurationData(
//            DEFAULT_NIBSS_IP,
//            DEFAULT_NIBSS_PORT.toString(),
//            DEFAULT_TERMINAL_ID,
//            Keys.liveKey1,
//            Keys.liveKey2
//        )
    }

    fun getKeyHolder(): KeyHolder? =
        gson.fromJson(Prefs.getString(PREF_KEYHOLDER, null), KeyHolder::class.java)

}

var TransactionResponse.additionalAmount: Long?
    get() = 0
    set(value) {

    }