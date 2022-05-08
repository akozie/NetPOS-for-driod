package com.woleapp.netpos.app

import android.app.Activity
import android.app.Application
import android.content.ContextWrapper
import com.mastercard.terminalsdk.ConfigurationInterface
import com.mastercard.terminalsdk.TerminalSdk
import com.mastercard.terminalsdk.TransactionInterface
import com.oluwatayo.taponphone.implementations.TransactionProcessLoggerImpl
import com.pixplicity.easyprefs.library.Prefs
import com.visa.app.ttpkernel.ContactlessConfiguration
import com.woleapp.netpos.taponphone.mastercard.implementations.*
import com.woleapp.netpos.taponphone.mastercard.implementations.nfc.NfcProvider
import timber.log.Timber

class NetPosApp : Application() {

    lateinit var transactionsApi: TransactionInterface
    lateinit var outcomeObserver: OutcomeObserver
    private lateinit var configuration: ConfigurationInterface
    lateinit var builder: StringBuilder
    lateinit var nfcProvider: NfcProvider

    companion object {
        lateinit var INSTANCE: NetPosApp

        fun assignInstance(instance: NetPosApp) {
            INSTANCE = instance
        }
    }

    val terminalSdk: TerminalSdk = TerminalSdk.getInstance()

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
        initVisaLib()
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

    private fun initVisaLib() {
        val contactlessConfiguration = ContactlessConfiguration.getInstance()
        val myData = contactlessConfiguration.terminalData
        Timber.e("data at start")
        myData.forEach {
            Timber.e(it.key)
        }
        myData["9F1A"] = byteArrayOf(0x05, 0x66) // set terminal country code
        myData["5F2A"] = byteArrayOf(0x05, 0x66) // set currency code
        myData["9F35"] = byteArrayOf(0x22) //Terminal Type
        myData["009C"] = byteArrayOf(0x00) //Transaction Type 00 - Purchase; 20 - Refund
        myData["9F09"] = byteArrayOf(0x00, 0x8C.toByte())
        myData["9F66"] =
            byteArrayOf(0xE6.toByte(), 0x00.toByte(), 0x40.toByte(), 0x00.toByte()) //TTQ E6004000
        myData["9F33"] =
            byteArrayOf(0xE0.toByte(), 0xF8.toByte(), 0xC8.toByte()) //Terminal Capabilities
        myData["9F40"] = byteArrayOf(
            0x60.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x50.toByte(), 0x01.toByte()
        ) //Additional Terminal Capabilities
        val mercahnt = "NetPOS Contactless"
        val merchantByte = mercahnt.toByteArray()
        myData["9F4E"] = merchantByte //Merchant Name and location
        myData["9F1B"] = byteArrayOf(0x00, 0x00, 0x00, 0x00) //terminal floor limit
        myData["DF01"] = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x01) //Reader CVM Required Limit
        contactlessConfiguration.terminalData = myData
    }

    fun initMposLibrary(context: Activity) {
        outcomeObserver = OutcomeObserver()
        configuration = terminalSdk.configuration
        builder = StringBuilder()
        nfcProvider = NfcProvider(context)
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
}