package com.woleapp.netpos.app

import android.app.Application
import android.content.ContextWrapper
import com.netplus.sunyardlib.SunyardLib
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import timber.log.Timber


class NetPosApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        NetPosTerminalConfig.init(this)
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()
        SunyardLib.init(this)
    }
}