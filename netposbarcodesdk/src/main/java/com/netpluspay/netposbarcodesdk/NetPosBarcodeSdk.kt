package com.netpluspay.netposbarcodesdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

object NetPosBarcodeSdk {

    fun startScan(
        context: Context,
        resultLauncher: ActivityResultLauncher<Intent>
    ) {
        BarcodeActivity.startActivity(context, resultLauncher)
    }
    
    fun startScan(context: Activity, requestCode: Int){
        BarcodeActivity.startActivity(context, requestCode)
    }
}