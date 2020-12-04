package com.woleapp.netpos.util

import com.danbamitale.epmslib.entities.ConfigData
import com.danbamitale.epmslib.entities.DEFAULT_NIBSS_IP
import com.danbamitale.epmslib.entities.DEFAULT_NIBSS_PORT
import com.danbamitale.epmslib.entities.KeyHolder
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.ConfigurationData
import com.woleapp.netpos.model.User
import com.woleapp.netpos.nibss.DEFAULT_TERMINAL_ID
import com.woleapp.netpos.nibss.Keys

object Singletons {
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
            DEFAULT_NIBSS_IP,
            DEFAULT_NIBSS_PORT.toString(),
            DEFAULT_TERMINAL_ID,
            Keys.liveKey1,
            Keys.liveKey2
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

    fun getConfigData(): ConfigData? =
        gson.fromJson(Prefs.getString(PREF_CONFIG_DATA, null), ConfigData::class.java)


}