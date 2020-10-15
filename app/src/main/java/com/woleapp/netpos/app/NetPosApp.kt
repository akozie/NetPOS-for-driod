package com.woleapp.netpos.app

import android.app.Application
import android.content.ContextWrapper
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import timber.log.Timber


class NetPosApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Timber.e("LMAOOOOO, e wan crash")
            Timber.e(e)
            throw e
        }
        NetPosTerminalConfig.init(applicationContext)
        //SunyardLib.init(this)
    }
}