package com.woleapp.netpos.nibss

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateUtils
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.netpluspay.nibssclient.exception.NibssClientException
import com.netpluspay.nibssclient.models.ConfigurationParams
import com.netpluspay.nibssclient.models.KeyHolder
import com.netpluspay.nibssclient.service.NibssApiWrapper
import com.netpluspay.nibssclient.util.app.NibssClient
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.*
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.Singletons.getSavedConfigurationData
import com.woleapp.netpos.util.Singletons.gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

const val CONFIGURATION_STATUS = "terminal_configuration_status"
const val CONFIGURATION_ACTION = "com.woleapp.netpos.TERMINAL_CONFIGURATION"
const val DEFAULT_TERMINAL_ID = "2057H63U"

object NetPosTerminalConfig {
    private var configurationData: ConfigurationData = getSavedConfigurationData()
    private val disposables = CompositeDisposable()
    private var terminalId: String? = null
    var isConfigurationInProcess = false
    var configurationStatus = -1
    private val mutableLiveData = MutableLiveData(Event(-99))
    val liveData: LiveData<Event<Int>>
        get() = mutableLiveData
    private val sendIntent = Intent(CONFIGURATION_ACTION)
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var configureSilently: Boolean = false

    fun getTerminalId() = terminalId ?: ""
    private fun setTerminalId(configurationData: ConfigurationData) {
        Timber.e("use storm TID ${useStormTerminalId()}")
        terminalId =
            if (useStormTerminalId()) Singletons.getCurrentlyLoggedInUser()?.terminal_id else configurationData.terminalId
    }

    private var keyHolder: KeyHolder? = null

    fun init(
        context: Context,
        newConfigurationData: ConfigurationData? = null,
        configureSilently: Boolean = false
    ) {
        this.configureSilently = configureSilently
        if (::localBroadcastManager.isInitialized.not())
            localBroadcastManager = LocalBroadcastManager.getInstance(context)
        newConfigurationData?.let { it ->
            configurationData = it
        }

        Timber.e("configure silently: $configureSilently")
        setTerminalId(configurationData)
        val terminalID = getTerminalId()
        NibssClient.init(
            "netpos.cert.pem",
            "private.key.pem",
            terminalID,
            Build.ID
        )
        NibssClient.useSSL(true)
        NibssClient.useTestEnvironment(false)
        Timber.e("Terminal ID: $terminalId")
        if (isConfigurationInProcess)
            return
        keyHolder = Singletons.getKeyHolder()
        val req = when {
            DateUtils.isToday(Prefs.getLong(LAST_POS_CONFIGURATION_TIME, 0)).not() -> {
                Timber.e("last configuration time was not today, configure terminal now")
                configureTerminal(context)
            }
            keyHolder != null -> {
                Timber.e("calling home")
                configurationStatus = 1
                callHome(context).flatMap {
                    if ((it == "00").not())
                        throw NibssClientException("Call home failed", null)
                    Single.just(KeyHolder())
                }.onErrorResumeNext {
                    Timber.e("call home failed, configure terminal")
                    configureTerminal(context)
                }
            }
            else -> configureTerminal(context)
        }
        req.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                isConfigurationInProcess = false
            }
            .subscribe { t1, t2 ->
                t1?.let {
                    if (it.configureTerminal) {
                        Prefs.putLong(LAST_POS_CONFIGURATION_TIME, System.currentTimeMillis())
                        Timber.e(it.toString())
                        Prefs.putString(PREF_KEYHOLDER, gson.toJson(it))
                        configurationStatus = 1
                        sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
                        localBroadcastManager.sendBroadcast(sendIntent)
                        if (configureSilently.not()) {
                            mutableLiveData.value = Event(configurationStatus)
                            mutableLiveData.value = Event(-99)
                        }
                        Timber.e("Config data set")
                    } else
                        Timber.e("call home success")
                    disposeDisposables()
                }
                t2?.let {
                    if (it is NibssClientException) {
                        it.nibssError?.let { nibssError -> Timber.e(nibssError.toString()) }
                    }
                    Toast.makeText(
                        context,
                        it.message ?: "Configuration Error",
                        Toast.LENGTH_SHORT
                    ).show()
                    configurationStatus = -1
                    if (configureSilently.not()) {
                        mutableLiveData.value = Event(configurationStatus)
                        mutableLiveData.value = Event(-99)
                    }
                    sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
                    localBroadcastManager.sendBroadcast(sendIntent)
                    Timber.e(it)
                }
            }.disposeWith(disposables)
    }

    private fun configureTerminal(context: Context): Single<KeyHolder> {
        Prefs.remove(PREF_KEYHOLDER)
        isConfigurationInProcess = true
        configurationStatus = 0
        sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
        localBroadcastManager.sendBroadcast(sendIntent)
        if (configureSilently.not()) {
            mutableLiveData.postValue(Event(configurationStatus))
            mutableLiveData.postValue(Event(-99))
        }
        return NibssApiWrapper.configureTerminal(context, ConfigurationParams())
    }


    private fun callHome(context: Context): Single<String> =
        NibssApiWrapper.callHome(context, ConfigurationParams())

    private val KeyHolder.configureTerminal
        get() = this.clearPinKey.isNullOrEmpty().not()

    private fun disposeDisposables() {
        disposables.clear()
    }
}
