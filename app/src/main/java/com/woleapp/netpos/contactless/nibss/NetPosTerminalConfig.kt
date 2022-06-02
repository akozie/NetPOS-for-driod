package com.woleapp.netpos.contactless.nibss

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.processors.TerminalConfigurator
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.model.ConfigurationData
import com.woleapp.netpos.contactless.util.*
import com.woleapp.netpos.contactless.util.Singletons.getSavedConfigurationData
import com.woleapp.netpos.contactless.util.Singletons.gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


//const val NIBSS_TEST_IP = "196.6.103.72"
//const val NIBSS_PROD_IP = "196.6.103.73"
//const val TERMINAL_SERIAL = "0123456789ABC"
const val CONFIGURATION_STATUS = "terminal_configuration_status"
const val CONFIGURATION_ACTION = "com.woleapp.netpos.TERMINAL_CONFIGURATION"
//const val DEFAULT_TERMINAL_ID = "2057H63U"

class NetPosTerminalConfig {
    companion object {
        private var configurationData: ConfigurationData = getSavedConfigurationData()
        private val disposables = CompositeDisposable()

        //        var connectionData: ConnectionData = ConnectionData(
//            ipAddress = "196.6.103.10",
//            ipPort = 55533,
//            isSSL = true
//        )
        var connectionData: ConnectionData = ConnectionData(
            ipAddress = configurationData.ip,
            ipPort = configurationData.port.toInt(),
            isSSL = true
        )
        private var terminalId: String? = null
        var isConfigurationInProcess = false
        var configurationStatus = -1
        private val mutableLiveData = MutableLiveData(Event(-99))
        val liveData: LiveData<Event<Int>>
            get() = mutableLiveData
        private val sendIntent = Intent(CONFIGURATION_ACTION)
        private var terminalConfigurator: TerminalConfigurator =
            TerminalConfigurator(connectionData)

        fun getTerminalId() = terminalId ?: ""


        private fun setTerminalId() {
            terminalId = Singletons.getCurrentlyLoggedInUser()?.terminal_id
        }

        private var keyHolder: KeyHolder? = null

        private var configData: ConfigData? = null

        fun getConfigData(): ConfigData? = configData

        fun getKeyHolder(): KeyHolder? = keyHolder

        fun init(
            context: Context,
            configureSilently: Boolean = false
        ) {
            KeyHolder.setHostKeyComponents(
                configurationData.key1,
                configurationData.key2
            )// default to test  //Set your base keys here

            setTerminalId()
            Timber.e("Terminal ID: $terminalId")
            keyHolder = Singletons.getKeyHolder()
            configData = Singletons.getConfigData()
            val localBroadcastManager = LocalBroadcastManager.getInstance(context)
            if (isConfigurationInProcess)
                return
            configurationStatus = 0
            sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
            localBroadcastManager.sendBroadcast(sendIntent)
            if (configureSilently.not()) {
                mutableLiveData.value = Event(configurationStatus)
                mutableLiveData.value = Event(-99)
            }
            val req = when {
                DateUtils.isToday(Prefs.getLong(LAST_POS_CONFIGURATION_TIME, 0)).not() -> {
                    Timber.e("last configuration time was not today, configure terminal now")
                    configureTerminal(context)
                }
                keyHolder != null && configData != null -> {
                    Timber.e("calling home")
                    configurationStatus = 1
                    callHome(context).onErrorResumeNext {
                        Timber.e(it)
                        Timber.e("call home failed, configure terminal")
                        configureTerminal(context)
                    }
                }
                else -> configureTerminal(context)
            }
            val disposable = req.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    isConfigurationInProcess = true
                }
                .doFinally { isConfigurationInProcess = false }
                .subscribe { pair, error ->
                    error?.let {
                        //TerminalManager.getInstance().beep(context, TerminalManager.BEEP_MODE_FAILURE)
                        configurationStatus = -1
                        if (configureSilently.not()) {
                            mutableLiveData.value = Event(configurationStatus)
                            mutableLiveData.value = Event(-99)
                        }
                        sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
                        localBroadcastManager.sendBroadcast(sendIntent)
                        Timber.e(it)
                    }
                    pair?.let {
                        pair.first?.let {
                            Prefs.putLong(LAST_POS_CONFIGURATION_TIME, System.currentTimeMillis())
                            Prefs.putString(PREF_CONFIG_DATA, gson.toJson(pair.second))
                            Prefs.putString(PREF_KEYHOLDER, gson.toJson(pair.first))
                            this.configData = pair.second
                        }
                        configurationStatus = 1
                        sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
                        localBroadcastManager.sendBroadcast(sendIntent)
                        if (configureSilently.not()) {
                            mutableLiveData.value = Event(configurationStatus)
                            mutableLiveData.value = Event(-99)
                        }
                        Timber.e("Config data set")
                        disposeDisposables()
                    }
                }
            disposables.add(disposable)
        }

        private fun callHome(context: Context): Single<Pair<KeyHolder?, ConfigData?>> {
            Timber.e(keyHolder.toString())
            return terminalConfigurator.nibssCallHome(
                context,
                getTerminalId(),
                keyHolder?.clearSessionKey ?: "",
                Build.ID
            ).flatMap {
                Timber.e("call home result $it")
                if (it == "00")
                    return@flatMap Single.just(Pair(null, null))
                else Single.error(Exception("call home failed"))
            }
        }

        private fun configureTerminal(context: Context): Single<Pair<KeyHolder?, ConfigData?>> =
            terminalConfigurator.downloadNibssKeys(context, getTerminalId())
                .flatMap { nibssKeyHolder ->
                    keyHolder = nibssKeyHolder
                    terminalConfigurator.downloadTerminalParameters(
                        context,
                        getTerminalId(),
                        nibssKeyHolder.clearSessionKey,
                        Build.ID
                    ).map { nibssConfigData ->
                        configData = nibssConfigData
                        return@map Pair(nibssKeyHolder, nibssConfigData)
                    }
                }


        private fun disposeDisposables() {
            disposables.clear()
        }
    }

}