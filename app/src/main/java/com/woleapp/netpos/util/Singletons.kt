package com.woleapp.netpos.util

import com.danbamitale.epmslib.entities.ConfigData
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.responseMessage
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.ConfigurationData
import com.woleapp.netpos.model.NibssResponse
import com.woleapp.netpos.model.User
import com.woleapp.netpos.nibss.Keys
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
        return ConfigurationData(
            "196.6.103.18",
            "5016",
            Keys.posvasLiveKey1,
            Keys.posvasLiveKey2,
        )
    }

    fun getKeyHolder(): com.danbamitale.epmslib.entities.KeyHolder? =
        gson.fromJson(Prefs.getString(PREF_KEYHOLDER, null), com.danbamitale.epmslib.entities.KeyHolder::class.java)

    fun getConfigData(): ConfigData? =
        gson.fromJson(Prefs.getString(PREF_CONFIG_DATA, null), ConfigData::class.java)

}

var TransactionResponse.additionalAmount: Long?
    get() = 0
    set(value) {

    }