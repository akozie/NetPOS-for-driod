package com.woleapp.netpos.app

import android.app.Application
import android.content.ContextWrapper
import com.netpluspay.netpossdk.NetPosSdk
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
            Prefs.putBoolean("load_provided", true)
        }


//        if (Prefs.contains("has_setup_device").not()){
//            NetPosSdk.loadEmvParams(
//                TerminalParameters()
//                    .apply {
//                        terminalCapability = "E0F8C8"
//                    }
//            )
//            NetPosSdk.loadProvidedCapksAndAids()
//        }
    }
}