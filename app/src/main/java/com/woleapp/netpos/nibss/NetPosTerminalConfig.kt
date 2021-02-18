package com.woleapp.netpos.nibss

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.processors.TerminalConfigurator
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.NetPosSdk.writeTpkKey
import com.netpluspay.netpossdk.utils.DeviceConfig
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.ConfigurationData
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.Singletons.getSavedConfigurationData
import com.woleapp.netpos.util.Singletons.gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


//const val NIBSS_TEST_IP = "196.6.103.72"
//const val NIBSS_PROD_IP = "196.6.103.73"
//const val TERMINAL_SERIAL = "0123456789ABC"
const val CONFIGURATION_STATUS = "terminal_configuration_status"
const val CONFIGURATION_ACTION = "com.woleapp.netpos.TERMINAL_CONFIGURATION"
const val DEFAULT_TERMINAL_ID = "2057H63U"

class NetPosTerminalConfig {
    companion object {
        private var configurationData: ConfigurationData = getSavedConfigurationData()
        private val disposables = CompositeDisposable()
        private var connectionData: ConnectionData? = null
        private var terminalId: String? = null
        var isConfigurationInProcess = false
        var configurationStatus = -1
        private val mutableLiveData = MutableLiveData(Event(-99))
        val liveData: LiveData<Event<Int>>
            get() = mutableLiveData
        private val sendIntent = Intent(CONFIGURATION_ACTION)

        fun getTerminalId() = terminalId ?: ""


        private fun setTerminalId(configurationData: ConfigurationData) {
            Timber.e("use storm TID ${useStormTerminalId()}")
            terminalId =
                if (useStormTerminalId()) Singletons.getCurrentlyLoggedInUser()?.terminal_id else configurationData.terminalId
        }

        private fun setConnectionData(configurationData: ConfigurationData) {
            connectionData = ConnectionData(
                ipAddress = configurationData.ip,
                ipPort = configurationData.port.toInt(),
                isSSL = true
            )
        }

        fun getConnectionData() = connectionData!!

        private var keyHolder: KeyHolder? = null

        private var configData: ConfigData? = null

        fun getConfigData(): ConfigData? = configData

        fun getKeyHolder(): KeyHolder? = keyHolder

        fun init(
            context: Context,
            newConfigurationData: ConfigurationData? = null,
            configureSilently: Boolean = false
        ) {
            KeyHolder.setHostKeyComponents(
                configurationData.key1,
                configurationData.key2
            )// default to test  //Set your base keys here
            Timber.e("configure silently: $configureSilently")
            newConfigurationData?.let { it ->
                configurationData = it
            }
            Timber.e(configurationData.toString())
            setConnectionData(configurationData)
            setTerminalId(configurationData)
            Timber.e("Terminal ID: $terminalId")
            keyHolder = Singletons.getKeyHolder()
            configData = Singletons.getConfigData()
            if (keyHolder != null && configData != null) {
                configurationStatus = 1
                Timber.e("done here")
                return
            }
            val localBroadcastManager = LocalBroadcastManager.getInstance(context)
            if (isConfigurationInProcess)
                return
            configurationStatus = 0
            sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
            localBroadcastManager.sendBroadcast(sendIntent)
            if (configureSilently.not()){
                mutableLiveData.value = Event(configurationStatus)
                mutableLiveData.value = Event(-99)
            }
            val configurator = TerminalConfigurator(getConnectionData())
            val terminalID = getTerminalId()
            //Toast.makeText(context, terminalID, Toast.LENGTH_LONG).show()
            val disposable = configurator.downloadNibssKeys(context, terminalID)
                .flatMap {
                    Timber.e("key holder set")
                    keyHolder = it
                    configurator.downloadTerminalParameters(
                        context,
                        terminalID,
                        it.clearSessionKey,
                        NetPosSdk.getDeviceSerial()
                    )
                }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    isConfigurationInProcess = true
                }
                .doFinally { isConfigurationInProcess = false }
                .subscribe { configData, error ->
                    error?.let {
                        //TerminalManager.getInstance().beep(context, TerminalManager.BEEP_MODE_FAILURE)
                        configurationStatus = -1
                        if (configureSilently.not()){
                            mutableLiveData.value = Event(configurationStatus)
                            mutableLiveData.value = Event(-99)
                        }
                        sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
                        localBroadcastManager.sendBroadcast(sendIntent)
                        Timber.e(it)
                    }
                    configData?.let {
                        //TerminalManager.getInstance().beep(context, TerminalManager.BEEP_MODE_SUCCESS)
                        Prefs.putString(PREF_CONFIG_DATA, gson.toJson(it))
                        Prefs.putString(PREF_KEYHOLDER, gson.toJson(keyHolder))
                        configurationStatus = 1
                        writeTpkKey(DeviceConfig.TPKIndex, keyHolder!!.clearPinKey)
                        sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
                        localBroadcastManager.sendBroadcast(sendIntent)
                        if (configureSilently.not()){
                            mutableLiveData.value = Event(configurationStatus)
                            mutableLiveData.value = Event(-99)
                        }
                        Timber.e("Config data set")
                        this.configData = it
                        disposeDisposables()
                    }
                }
            disposables.add(disposable)
        }


        private fun disposeDisposables() {
            disposables.clear()
        }
    }
}