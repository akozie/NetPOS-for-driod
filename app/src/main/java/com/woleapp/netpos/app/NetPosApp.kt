package com.woleapp.netpos.app

import android.app.Application
import android.content.ContextWrapper
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.utils.TerminalParameters
import com.netpluspay.netpossdk.utils.tlv.HexUtil
import com.pixplicity.easyprefs.library.Prefs
import timber.log.Timber

class NetPosApp : Application() {


    override fun onCreate() {
        super.onCreate()
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

        NetPosSdk.init()
        if (Prefs.contains("load_provided").not()) {
            NetPosSdk.loadProvidedCapksAndAids()
            NetPosSdk.loadEmvParams(
                TerminalParameters()
                    .apply {
                        //online E068C8
                        terminalCapability = "E0F8C8"
                    }
            )
            Prefs.putBoolean("load_provided", true)
        }
    }
}