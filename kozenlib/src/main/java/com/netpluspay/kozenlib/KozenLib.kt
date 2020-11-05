package com.netpluspay.kozenlib

import android.content.Context
import android.content.Intent
import com.netpluspay.kozenlib.emv.EmvParam
import com.pos.sdk.printer.POIPrinterManage

object KozenLib {

    private lateinit var printerManager: POIPrinterManage

    @JvmStatic
    fun init(context: Context) {
        printerManager = POIPrinterManage.getDefault(context)
        //loadEmvParams()
        //
    }

    @JvmStatic
    fun getPrinterManager(): POIPrinterManage {
        if (::printerManager.isInitialized.not())
            throw NullPointerException(
                "KozenLib has not been initialized, you must call " +
                        "KozenLib.init(context) first, this call needs to be made just once."
            )
        return printerManager
    }

    private fun loadEmvParams() {
        EmvParam.loadTerminalParam()
        EmvParam.loadVisaParam()
        EmvParam.loadMasterCardParam()
        //EmvParam.loadAmexParam()
        //EmvParam.loadDiscoverParam()
        //EmvParam.loadQuicsParam()
        EmvParam.loadVisaDrl()
        //EmvParam.loadAemxDrl()
        // EmvParam.loadRuPayService()
        EmvParam.loadAddAids()
        EmvParam.loadAddCapks()
    }
}