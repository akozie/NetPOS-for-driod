package com.woleapp.netpos.contactless.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SHUTDOWN -> {

            }
            Intent.ACTION_BOOT_COMPLETED -> {

            }
        }
    }
}