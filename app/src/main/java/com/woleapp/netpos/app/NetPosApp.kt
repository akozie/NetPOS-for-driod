package com.woleapp.netpos.app

import android.app.Application
import android.content.ContextWrapper
import android.os.Build
import com.google.gson.JsonObject
import com.netpluspay.kozenlib.KozenLib
import com.pixplicity.easyprefs.library.Prefs
import com.pos.sdk.accessory.POIGeneralAPI
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.util.Singletons.firestore
import com.woleapp.netpos.util.getBeginningOfDay
import com.woleapp.netpos.util.getEndOfDayTimeStamp
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
        /*Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Timber.e("LMAOOOOO, e wan crash")
            Timber.e(e)
            throw e
        }*/
        KozenLib.init(applicationContext)

        POIGeneralAPI.getDefault().apply {
            val jsonObject = HashMap<String, String>()
            jsonObject["HW"] = getVersion(POIGeneralAPI.VERSION_TYPE_HW)
            jsonObject["AP"] = getVersion(POIGeneralAPI.VERSION_TYPE_AP)
            jsonObject["DSN"] = getVersion(POIGeneralAPI.VERSION_TYPE_DSN)
            jsonObject["SP"] = getVersion(POIGeneralAPI.VERSION_TYPE_SP)
            firestore.collection("device_info${Build.MODEL}").document("p5_info").set(jsonObject)
                .addOnCompleteListener {

                }
        }
        //NetPosTerminalConfig.init(applicationContext)
        //DashboardFragment.printSampleReceipt()
        //SunyardLib.init(this)
//        AppDatabase.getDatabaseInstance(applicationContext)
//            .transactionResponseDao().getTransactions()
    }
}