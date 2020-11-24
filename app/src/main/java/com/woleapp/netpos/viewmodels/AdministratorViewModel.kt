package com.woleapp.netpos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.ConfigurationData
import com.woleapp.netpos.util.Event
import com.woleapp.netpos.util.PREF_CONFIGURATION_DATA
import com.woleapp.netpos.util.Singletons.getSavedConfigurationData
import com.woleapp.netpos.util.Singletons.gson
import com.woleapp.netpos.util.isValidIpAddress

class AdministratorViewModel : ViewModel() {
    private val _message = MutableLiveData<Event<String>>()
    private val _saveConfiguration = MutableLiveData<Event<Boolean>>()
    val configurationData: MutableLiveData<ConfigurationData> by lazy {
        MutableLiveData<ConfigurationData>().also {
            it.value = getSavedConfigurationData()
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
        if (validateInputs(configurationData.value!!)) {
            _message.value = Event("Please fill up all fields")
            return
        }
        _saveConfiguration.value = Event(true)
    }

    private fun validateInputs(configurationData: ConfigurationData): Boolean {
        configurationData.run {
            if (ip.isEmpty() || !ip.isValidIpAddress()) {
                //_message.value = Event("please enter a valid IP address")
                return false
            }
            if (port.isEmpty() || key1.isEmpty() || key2.isEmpty() || terminalId.isEmpty()) {
                _message.value = Event("Ensure all field are properly filled")
                return false
            }

        }
        return true
    }

    fun saveConfigurationToPreference() {
        Prefs.putString(PREF_CONFIGURATION_DATA, gson.toJson(configurationData.value))
    }
}