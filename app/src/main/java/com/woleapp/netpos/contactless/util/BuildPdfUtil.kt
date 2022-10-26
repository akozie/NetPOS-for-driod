package com.woleapp.netpos.contactless.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.FileProvider
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.danbamitale.epmslib.entities.TransactionResponse
import com.woleapp.netpos.contactless.BuildConfig
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.LayoutPosReceiptPdfBinding
import com.woleapp.netpos.contactless.databinding.LayoutQrReceiptPdfBinding
import com.woleapp.netpos.contactless.model.QrTransactionResponseFinalModel
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

fun initViewsForPdfLayout(
    pdfView: ViewDataBinding,
    receipt: Any?
) {
    when (receipt) {
        is QrTransactionResponseFinalModel -> {
            initViewsForQrReceipt((pdfView as LayoutQrReceiptPdfBinding), receipt)
        }
        is TransactionResponse -> {
            initViewsForPosReceipt((pdfView as LayoutPosReceiptPdfBinding), receipt)
        }
        else -> { /* Do nothing */
        }
    }
}

fun sharePdf(
    outputFile: File,
    host: LifecycleOwner
) {
    // This is the way to do it for targetSdkVersion < 24
//        val uri: Uri = Uri.fromFile(outputFile)
    // For targetSdkVersion >= 24
    val context: Context = if (host is Fragment) host.requireContext() else (host as Activity)
    val uri: Uri = FileProvider.getUriForFile(
        context,
        context.applicationContext?.packageName + ".provider",
        outputFile
    )
    val share = Intent().apply {
        action = Intent.ACTION_SEND
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
    }
    context.startActivity(share)
}

fun createPdf(
    pdfView: ViewDataBinding,
    host: LifecycleOwner
): File {
    val displayMetrics = DisplayMetrics()
    if (host is Fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            host.context?.display?.getRealMetrics(displayMetrics)
            displayMetrics.densityDpi
        } else {
            host.activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        }
    } else {
        host as Activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            host.display?.getRealMetrics(displayMetrics)
            displayMetrics.densityDpi
        } else {
            host.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        }
    }

    pdfView.root.measure(
        View.MeasureSpec.makeMeasureSpec(
            displayMetrics.widthPixels,
            View.MeasureSpec.EXACTLY
        ),
        View.MeasureSpec.makeMeasureSpec(
            displayMetrics.heightPixels,
            View.MeasureSpec.EXACTLY
        )
    )

    pdfView.root.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)

    val bitmap = pdfView.root.measuredWidth.let {
        pdfView.root.measuredHeight.let { it1 ->
            Bitmap.createBitmap(
                it,
                it1,
                Bitmap.Config.ARGB_8888
            )
        }
    }

    val canvas = bitmap?.let { Canvas(it) }
    pdfView.root.draw(canvas)

    if (bitmap != null) {
        Bitmap.createScaledBitmap(bitmap, 595, 842, true)
    }
    val pdfDocument = PdfDocument()
    val pageInfo = bitmap?.let {
        PdfDocument.PageInfo.Builder(it.width, it.height, 1).create()
    }
    val page = pdfDocument.startPage(pageInfo)
    if (bitmap != null) {
        page.canvas.drawBitmap(bitmap, 0F, 0F, null)
    }
    pdfDocument.finishPage(page)
    val filePath = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "NetPOS_Receipt_" + getCurrentDateTimeAsFormattedString() + ".pdf"
    )

    pdfDocument.writeTo(FileOutputStream(filePath))
    pdfDocument.close()

    return filePath
}

private fun initViewsForQrReceipt(
    pdfView: LayoutQrReceiptPdfBinding,
    responseFromWebView: QrTransactionResponseFinalModel?
) {
    pdfView.apply {
        responseFromWebView?.let { respFromWebView: QrTransactionResponseFinalModel ->
            merchantName.text = pdfView.root.context.getString(
                R.string.merchant_name_place_holder,
                respFromWebView.customerName
            )
            terminalIdPlaceHolder.text =
                pdfView.appVersion.context.getString(
                    R.string.terminal_id_place_holder,
                    respFromWebView.terminalId
                )
            dateTime.text =
                pdfView.appVersion.context.getString(
                    R.string.date_time_place_holder,
                    respFromWebView.transmissionDateTime
                )
            transAmount.text = pdfView.appVersion.context.getString(
                R.string.amount_place_holder,
                respFromWebView.amount.toDouble().formatCurrencyAmountUsingCurrentModule()
            )
            orderId.text = pdfView.appVersion.context.getString(
                R.string.order_id_place_holder,
                respFromWebView.rrnOrderId
            )
            narration.text =
                pdfView.appVersion.context.getString(
                    R.string.narration_place_holder,
                    respFromWebView.narration
                )
            transId.text =
                pdfView.appVersion.context.getString(
                    R.string.trans_ref_place_holder,
                    respFromWebView.transIdStan
                )
            status.text =
                pdfView.appVersion.context.getString(
                    R.string.transaction_status_place_holder,
                    respFromWebView.status.uppercase()
                )
            responseCode.text =
                pdfView.appVersion.context.getString(
                    R.string.response_code_place_holder,
                    respFromWebView.responseCode
                )
            message.text = pdfView.appVersion.context.getString(
                R.string.message_place_holder,
                respFromWebView.message
            )
            appVersion.text = pdfView.appVersion.context.getString(
                R.string.app_version_place_holder,
                "${BuildConfig.FLAVOR} POS ${BuildConfig.VERSION_NAME}"
            )
        }
    }
}

private fun initViewsForPosReceipt(
    pdfView: LayoutPosReceiptPdfBinding,
    transResponse: TransactionResponse
) {
    pdfView.apply {
        transResponse.let {
            merchantName.text = pdfView.root.context.getString(
                R.string.merchant_name_place_holder,
                Singletons.getCurrentlyLoggedInUser()?.business_name
                    ?: "${BuildConfig.FLAVOR} POS MERCHANT"
            )
            terminalIdPlaceHolder.text =
                pdfView.appVersion.context.getString(
                    R.string.terminal_id_place_holder,
                    it.terminalId
                )
            dateTime.text =
                pdfView.appVersion.context.getString(
                    R.string.date_time_place_holder,
                    it.transactionTimeInMillis.formatDate()
                )
            transAmount.text = pdfView.appVersion.context.getString(
                R.string.amount_place_holder,
                it.amount.div(100).toDouble().formatCurrencyAmountUsingCurrentModule()
            )
            stan.text =
                pdfView.appVersion.context.getString(
                    R.string.stan_place_holder,
                    it.STAN
                )
            cardHolder.text =
                pdfView.appVersion.context.getString(
                    R.string.card_holder_place_holder,
                    it.cardHolder
                )
            status.text =
                pdfView.appVersion.context.getString(
                    R.string.transaction_status_place_holder,
                    if (it.responseCode == "00" || it.responseCode == "16") "APPROVED" else "DECLINED"
                )
            responseCode.text =
                pdfView.appVersion.context.getString(
                    R.string.response_code_place_holder,
                    it.responseCode
                )
            message.text = pdfView.appVersion.context.getString(
                R.string.message_place_holder,
                it.otherId
            )
            cardType.text =
                pdfView.appVersion.context.getString(R.string.card_type_place_holder, it.cardLabel)
            appVersion.text = pdfView.appVersion.context.getString(
                R.string.app_version_place_holder,
                "${BuildConfig.FLAVOR} POS ${BuildConfig.VERSION_NAME}"
            )
        }
    }
}

private fun Number.formatCurrencyAmountUsingCurrentModule(currencySymbol: String = "\u20A6"): String {
    val format = DecimalFormat("#,###.00")
    return "$currencySymbol${format.format(this)}"
}
