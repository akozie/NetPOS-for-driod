package com.netpluspay.kozenlib.printer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.netpluspay.kozenlib.KozenLib
import com.netpluspay.kozenlib.R
import com.netpluspay.terminalcore.AndroidTerminalReceiptBuilderFactory
import com.pos.sdk.printer.IPosPrinterListener
import com.pos.sdk.printer.POIPrinterManage
import com.pos.sdk.printer.models.BitmapPrintLine
import com.pos.sdk.printer.models.PrintLine
import com.pos.sdk.printer.models.TextPrintLine
import io.reactivex.Single


class ReceiptBuilder :
    AndroidTerminalReceiptBuilderFactory<ReceiptBuilder, Single<PrinterResponse>>() {

    private val textPrintLine = TextPrintLine().apply {
        type = PrintLine.TEXT
    }
    private val printerManager = KozenLib.getPrinterManager().apply {
        setPrintGray(Integer.valueOf("4000"))
        setLineSpace(Integer.valueOf("1"))
        cleanCache()
    }

    override fun getThis(): ReceiptBuilder = this


    override fun appendTextEntity(p0: String?) {
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

    override fun appendTextEntityBold(p0: String?) {
        printerManager.addPrintLine(
            textPrintLine.apply {
                content = p0
                position = PrintLine.LEFT
                size = TextPrintLine.FONT_NORMAL
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
                isBold = true
                isItalic = false
                isInvert = false
            })
    }

    override fun appendTextEntityCenter(p0: String?) {
        printerManager.addPrintLine(
            textPrintLine.apply {
                content = p0
                position = PrintLine.CENTER
                size = TextPrintLine.FONT_NORMAL
                isBold = false
                isItalic = false
                isInvert = false
            })
    }

    fun print(printerListener: POIPrinterManage.IPrinterListener){
        build()
        printerManager.beginPrint(printerListener)
    }

    override fun print(): Single<PrinterResponse> =
        Single.create {
            build()
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

    override fun appendImageCenter() {
        val bitmapPrintLine = BitmapPrintLine()
        bitmapPrintLine.type = PrintLine.BITMAP
        bitmapPrintLine.position = PrintLine.CENTER
        val bitmap: Bitmap = BitmapFactory.decodeResource(KozenLib.getContext().resources, R.drawable.ic_netpos_logo)
        bitmapPrintLine.bitmap = Bitmap.createScaledBitmap(bitmap, 120, 120, false)
        printerManager.addPrintLine(bitmapPrintLine)
    }
}
