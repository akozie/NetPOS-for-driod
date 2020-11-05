package com.netpluspay.kozenlib.utils

import java.lang.Exception

class POSException(private val errorCode: Int, private val errorMessage:String): Exception() {
    override fun getLocalizedMessage(): String? = "POS exception with code: $errorCode - $errorMessage"

    override val message: String?
        get() = errorMessage
}