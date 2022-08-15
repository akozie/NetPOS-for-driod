package com.netpluspay.netposbarcodesdk

internal interface QRCodeFoundListener {
    fun onQrCodeFound(text: String)
    fun onQrCodeNotFound()
}