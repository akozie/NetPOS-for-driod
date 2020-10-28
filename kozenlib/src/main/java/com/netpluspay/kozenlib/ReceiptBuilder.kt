package com.netpluspay.kozenlib

import android.content.Context
import android.util.Log
import com.netpluspay.terminal_core.AndroidTerminalReceiptBuilderFactory
import com.pos.sdk.printer.POIPrinterManage
import com.pos.sdk.printer.models.PrintLine
import com.pos.sdk.printer.models.TextPrintLine
import io.reactivex.Single

class ReceiptBuilder(context: Context? = null) :
    AndroidTerminalReceiptBuilderFactory<ReceiptBuilder, Single<PrinterResponse>>() {
    private val printerManager: POIPrinterManage = POIPrinterManage.getDefault(context)
    private val textPrintLine = TextPrintLine().apply {
        type = PrintLine.TEXT
    }

    override fun getThis(): ReceiptBuilder = this

    override fun appendTextEntity(p0: String?) {
        printerManager.addPrintLine(
            textPrintLine.apply {
                content = p0
                position = PrintLine.LEFT
                size = TextPrintLine.FONT_SMALL
                isBold = false
                isItalic = false
                isInvert = false
            })
    }

    override fun appendTextEntityBold(p0: String?) {
        printerManager.addPrintLine(
            textPrintLine.apply {
                content = p0
                position = PrintLine.LEFT
                size = TextPrintLine.FONT_SMALL
                isBold = true
                isItalic = false
                isInvert = false
            })
    }

    override fun appendTextEntityFontSixteen(p0: String?) {
        printerManager.addPrintLine(
            textPrintLine.apply {
                content = p0
                position = PrintLine.LEFT
                size = TextPrintLine.FONT_NORMAL
                isBold = false
                isItalic = false
                isInvert = false
            })
    }

    override fun appendTextEntityFontSixteenCenter(p0: String?) {
        printerManager.addPrintLine(
            textPrintLine.apply {
                content = p0
                position = PrintLine.CENTER
                size = TextPrintLine.FONT_LARGE
                isBold = false
                isItalic = false
                isInvert = false
            })
    }

    override fun appendTextEntityCenter(p0: String?) {
        printerManager.addPrintLine(
            textPrintLine.apply {
                content = p0
                position = PrintLine.CENTER
                size = TextPrintLine.FONT_SMALL
                isBold = false
                isItalic = false
                isInvert = false
            })
    }

    override fun print(): Single<PrinterResponse> =
        Single.create {
            printerManager.beginPrint(object : POIPrinterManage.IPrinterListener {
                override fun onError(p0: Int, p1: String?) {
                    it.onError(Throwable("message:$p1 - code:$p0"))
                }

                override fun onFinish() {
                    it.onSuccess(PrinterResponse())
                }

                override fun onStart() {
                    Log.e("TAG", "Printing started")
                }

            })
        }
}