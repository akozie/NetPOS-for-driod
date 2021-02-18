package com.woleapp.netpos.app

import android.app.Application
import android.content.ContextWrapper
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.printer.ReceiptBuilder
import com.netpluspay.netpossdk.utils.TerminalParameters
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.mqtt.MqttHelper
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit


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
        Prefs.remove("HAS_WRITTEN_WRONG_KEY")
        /*Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Timber.e("LMAOOOOO, e wan crash")
            Timber.e(e)
            throw e
        }*/
//        ProcessLifecycleOwner.get().lifecycle
//            .addObserver(AppLifeCycleObserver())


        NetPosSdk.init()
        if (Prefs.contains("load_provided").not()) {
            //NetPosSdk.loadProvidedCapksAndAids()
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