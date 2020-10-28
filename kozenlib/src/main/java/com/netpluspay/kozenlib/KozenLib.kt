package com.netpluspay.kozenlib

import android.content.Context

class KozenLib {
    companion object {
        fun init(context: Context) {
            ReceiptBuilder.initialize(context)
        }
    }
}