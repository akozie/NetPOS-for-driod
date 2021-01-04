package com.woleapp.netpos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.ConfigurationData
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.Singletons.getSavedConfigurationData
import com.woleapp.netpos.util.Singletons.gson
import timber.log.Timber

class AdministratorViewModel : ViewModel() {
    private val _message = MutableLiveData<Event<String>>()
    private val _saveConfiguration = MutableLiveData<Event<Boolean>>()
    val configurationData: MutableLiveData<ConfigurationData> by lazy {
        MutableLiveData<ConfigurationData>().also {
            it.value = getSavedConfigurationData()
        }
    }
    val useStormTid: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>().also {
            it.value = useStormTerminalId()
        }
    }

    val saveConfigurationData: LiveData<Event<Boolean>>
        get() = _saveConfiguration

    val message: LiveData<Event<String>>
        get() = _message

    init {
        configurationData.value = getSavedConfigurationData()
    }

    fun saveConfiguration() {
        if (validateInputs(configurationData.value!!).not())
            return
        _saveConfiguration.value = Event(true)
    }

    private fun validateInputs(configurationData: ConfigurationData): Boolean {
        return configurationData.run {
            if (ip.isEmpty()) {
                _message.value = Event("please enter a valid IP address")
                return@run false
            } else if (port.isEmpty() || key1.isEmpty() || key2.isEmpty()) {
                _message.value = Event("Ensure all field are properly filled")
                return@run false
            } else if (useStormTid.value!!.not() && terminalId.isEmpty()) {
                _message.value = Event("Input a valid TID if not using TID from account")
                return@run false
            } else
                return@run true
        }
    }

    fun saveConfigurationToPreference() {
        Prefs.putString(PREF_CONFIGURATION_DATA, gson.toJson(configurationData.value))
        Singletons.setUseStormTid(useStormTid.value ?: true)
    }
}