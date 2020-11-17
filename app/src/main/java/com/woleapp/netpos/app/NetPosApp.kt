package com.woleapp.netpos.app

import android.app.Application
import android.content.ContextWrapper
import com.netpluspay.kozenlib.KozenLib
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.util.getBeginningOfDay
import com.woleapp.netpos.util.getEndOfDayTimeStamp
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
        /*Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Timber.e("LMAOOOOO, e wan crash")
            Timber.e(e)
            throw e
        }*/
        KozenLib.init(applicationContext)
        Timber.e(getBeginningOfDay().toString())
        Timber.e(getEndOfDayTimeStamp().toString())
        //NetPosTerminalConfig.init(applicationContext)
        //DashboardFragment.printSampleReceipt()
        //SunyardLib.init(this)
//        AppDatabase.getDatabaseInstance(applicationContext)
//            .transactionResponseDao().getTransactions()
    }
}