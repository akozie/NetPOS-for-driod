package com.netpluspay.kozenlib

import android.content.Context
import com.netpluspay.kozenlib.emv.EmvParam

class KozenLib {
    companion object {
        fun init(context: Context) {
            ReceiptBuilder.initialize(context)
            loadEmvParams()
        }

        private fun loadEmvParams(){
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
}