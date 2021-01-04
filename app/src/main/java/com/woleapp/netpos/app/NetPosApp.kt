package com.woleapp.netpos.app

import android.app.Application
import android.content.ContextWrapper
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.netpluspay.kozenlib.KozenLib
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.mqtt.MqttHelper
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber


class NetPosApp : Application() {

    inner class AppLifeCycleObserver() : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onMoveToForeground() {
            Timber.e("In foreground")
            MqttHelper.init(applicationContext)
            //Toast.makeText(applicationContext, "App in foreground", Toast.LENGTH_SHORT).show();
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onMoveToBackground() {
            MqttHelper.disconnect()
            Timber.e("Moved to background")
            //Toast.makeText(applicationContext, "App in background", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()
        RxJavaPlugins.setErrorHandler {
            Timber.e("Error: ${it.localizedMessage}")
        }
        Prefs.remove("HAS_WRITTEN_WRONG_KEY")
        /*Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Timber.e("LMAOOOOO, e wan crash")
            Timber.e(e)
            throw e
        }*/
//        ProcessLifecycleOwner.get().lifecycle
//            .addObserver(AppLifeCycleObserver())
        KozenLib.init(applicationContext)
        //NetPosTerminalConfig.init(applicationContext)
        //DashboardFragment.printSampleReceipt()
        //SunyardLib.init(this)
//        AppDatabase.getDatabaseInstance(applicationContext)
//            .transactionResponseDao().getTransactions()
    }
}