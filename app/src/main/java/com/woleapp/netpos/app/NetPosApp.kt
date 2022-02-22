package com.woleapp.netpos.app

import android.app.Activity
import android.app.Application
import android.content.ContextWrapper
import com.mastercard.terminalsdk.ConfigurationInterface
import com.mastercard.terminalsdk.TerminalSdk
import com.mastercard.terminalsdk.TransactionInterface
import com.oluwatayo.taponphone.implementations.TransactionProcessLoggerImpl
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.taponphone.implementations.*
import com.woleapp.netpos.taponphone.implementations.nfc.NfcProvider
import timber.log.Timber

class NetPosApp : Application() {

    lateinit var transactionsApi: TransactionInterface
    lateinit var outcomeObserver: OutcomeObserver
    lateinit var configuration: ConfigurationInterface
    lateinit var builder: StringBuilder

    companion object {
        lateinit var INSTANCE: NetPosApp

        fun assignInstance(instance: NetPosApp) {
            INSTANCE = instance
        }
    }

    val terminalSdk = TerminalSdk.getInstance()

    override fun onCreate() {
        super.onCreate()
        assignInstance(this)
        Timber.plant(Timber.DebugTree())
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()
        //TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
//        RxJavaPlugins.setErrorHandler {
//            Timber.e("Error: ${it.localizedMessage}")
//        }
        /*Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Timber.e("LMAOOOOO, e wan crash")
            Timber.e(e)
            throw e
        }*/
//        ProcessLifecycleOwner.get().lifecycle
//            .addObserver(AppLifeCycleObserver())

    }
    fun initMposLibrary(context: Activity) {
        outcomeObserver = OutcomeObserver()
        configuration = terminalSdk.configuration
        builder = StringBuilder()
        val nfcProvider = NfcProvider(context)
        val cardCommProviderStub = CardCommProviderStub()
        val logger = TransactionProcessLoggerImpl(builder)
        configuration
            .withResourceProvider(
                ResourceProviderImplementation(this.applicationContext)
            )
            .withLogger(logger)
            .withCardCommunication(nfcProvider, cardCommProviderStub)
            .withTransactionObserver(outcomeObserver)
            .withUnpredictableNumberProvider(UnpredictableNumberImplementation())
            .withMessageDisplayProvider(DisplayImplementation(logger))
        transactionsApi = configuration.initializeLibrary()
        configuration.selectProfile("MPOS")
    }

    fun logBuilderToOutLogcat(){
        println("-----BEGIN LOG-----")
        println(builder.toString())
        println("-----END LOG-----")
    }
}