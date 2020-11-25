package com.woleapp.netpos.nibss

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.processors.TerminalConfigurator
import com.netpluspay.kozenlib.utils.DeviceConfig
import com.netpluspay.kozenlib.utils.tlv.HexUtil
import com.pixplicity.easyprefs.library.Prefs
import com.pos.sdk.security.POIHsmManage
import com.pos.sdk.security.PedKcvInfo
import com.pos.sdk.security.PedKeyInfo
import com.woleapp.netpos.model.ConfigurationData
import com.woleapp.netpos.util.PREF_CONFIG_DATA
import com.woleapp.netpos.util.PREF_KEYHOLDER
import com.woleapp.netpos.util.Singletons
import com.woleapp.netpos.util.Singletons.getSavedConfigurationData
import com.woleapp.netpos.util.Singletons.gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


const val NIBSS_TEST_IP = "196.6.103.72"
const val NIBSS_PROD_IP = "196.6.103.73"
const val TERMINAL_SERIAL = "0123456789ABC"
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
        private val sendIntent = Intent(CONFIGURATION_ACTION)

        fun getTerminalId() = terminalId!!
        private var context: Context? = null


        private fun setTerminalId(configurationData: ConfigurationData) {
            terminalId = configurationData.terminalId
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

        fun init(context: Context, newConfigurationData: ConfigurationData? = null) {
            newConfigurationData?.let { it ->
                configurationData = it
            }
            Timber.e(configurationData.toString())
            setConnectionData(configurationData)
            setTerminalId(configurationData)
            Timber.e("Terminal ID: $terminalId")
            //keyHolder = Singletons.getKeyHolder()
            //configData = Singletons.getConfigData()
            if (keyHolder != null && configData != null) {
                Timber.e("done here")
                //return
            }
            val localBroadcastManager = LocalBroadcastManager.getInstance(context)
            if (isConfigurationInProcess)
                return
            configurationStatus = 0
            sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
            localBroadcastManager.sendBroadcast(sendIntent)
            this.context = context
            val configurator = TerminalConfigurator(getConnectionData())
            val terminalID = getTerminalId()

            val disposable = configurator.downloadNibssKeys(context, terminalID)
                .flatMap {
                    Timber.e("key holder set")
                    keyHolder = it
                    KeyHolder.setHostKeyComponents(
                        configurationData.key1,
                        configurationData.key2
                    )// default to test  //Set your base keys here
                    configurator.downloadTerminalParameters(
                        context,
                        terminalID,
                        it.clearSessionKey,
                        TERMINAL_SERIAL
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
                        sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
                        localBroadcastManager.sendBroadcast(sendIntent)
                        Timber.e(it)
                    }
                    configData?.let {
                        //TerminalManager.getInstance().beep(context, TerminalManager.BEEP_MODE_SUCCESS)

                        configurationStatus = 1
                        writeTpkKey(DeviceConfig.TPKIndex, keyHolder!!.clearPinKey)
                        sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
                        localBroadcastManager.sendBroadcast(sendIntent)
                        Timber.e("Config data set")
                        this.configData = it
                        Prefs.putString(PREF_CONFIG_DATA, gson.toJson(it))
                        Prefs.putString(PREF_KEYHOLDER, gson.toJson(keyHolder))
                    }
                }
            disposables.add(disposable)
        }

        private fun writeTpkKey(keyIndex: Int, keyData: String): Int {
            val pedKeyInfo = PedKeyInfo(
                0, 0, POIHsmManage.PED_TPK, keyIndex, 0, 16,
                HexUtil.parseHex(keyData)
            )
            return POIHsmManage.getDefault().PedWriteKey(pedKeyInfo, PedKcvInfo(0, ByteArray(5)))
        }

        var sampleCardData = CardData(
            track2Data = "4761739001010135D191220119559258",
            nibssIccSubset = "9F26088F8BFBE76089D66F9F2701809F10201F220100A48802000000000000000000000000000000000000000000000000009F3704389456479F360202A1950500800088009A031902189C01009F02060000000001005F2A020566820238009F1A0205669F34030103029F3303E0F9C89F3501519F1E0830303030303030318407A00000000310109F090200009F03060000000000005F3401018E10000000000000000001031E0302031F00",
            panSequenceNumber = "001", posEntryMode = "051"
        ).apply {
            pinBlock = null
        }//TODO get card information from terminal

        private fun disposeDisposables() {
            disposables.clear()
        }
    }
}