package com.netpluspay.kozenlib.printer

import com.pos.sdk.printer.POIPrinterManage

data class PrinterResponse(
    val code: Int = POIPrinterManage.Printer_Success,
    var message: String = "Success"
)