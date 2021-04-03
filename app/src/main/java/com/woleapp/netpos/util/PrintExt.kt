package com.woleapp.netpos.util

import android.content.Context
import android.graphics.BitmapFactory
import com.netpluspay.netpossdk.printer.PrinterResponse
import com.netpluspay.netpossdk.printer.ReceiptBuilder
import com.netpluspay.nibssclient.models.TransactionResponse
import com.netpluspay.nibssclient.models.responseMessage
import com.netpluspay.nibssclient.util.formatCurrencyAmount
import com.pos.sdk.printer.POIPrinterManage
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.R
import com.woleapp.netpos.model.NipNotification
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import timber.log.Timber
import java.lang.StringBuilder

fun List<TransactionResponse>.printAll(
    context: Context,
    isMerchantCopy: Boolean
): Observable<PrinterResponse> {
    var emitter: ObservableEmitter<PrinterResponse>? = null
    val printerListener = object : POIPrinterManage.IPrinterListener {
        override fun onError(p0: Int, p1: String?) {
            emitter?.let {
                if (it.isDisposed.not())
                    it.onError(Throwable("message: $p1 - code: $p0"))
            }
        }

        override fun onFinish() {
            emitter?.let {
                if (it.isDisposed.not())
                    it.onNext(PrinterResponse())
            }
        }

        override fun onStart() {
            Timber.e("Printing started")
        }
    }
    return Observable.create {
        emitter = it
        forEach { transactionResponse ->
            if (it.isDisposed.not())
                transactionResponse.print(printerListener, context, isMerchantCopy)
        }
        it.onComplete()
    }
}

fun TransactionResponse.print(
    printerListener: POIPrinterManage.IPrinterListener,
    context: Context,
    isMerchantCopy: Boolean = true
) {
    buildReceipt(context, isMerchantCopy).print(printerListener)
}

fun TransactionResponse.print(context: Context, remark: String? = null) =
    buildReceipt(remark = remark, context = context).print()

fun TransactionResponse.builder() = StringBuilder().apply {
    append("Merchant Name: ").append(Singletons.getCurrentlyLoggedInUser()!!.business_name)
    append("\nTERMINAL ID: ").append(terminalId).append("\n")
    append(transactionType).append("\n")
    append("DATE/TIME: ").append(transactionTimeInMillis.formatDate()).append("\n")
    append("AMOUNT: ").append(amount.div(100).formatCurrencyAmount("\u20A6")).append("\n")
    append(cardLabel).append(" Ending with ").append(maskedPan.substring(maskedPan.length - 4))
        .append("\n")
    append("RESPONSE CODE: ").append(responseCode).append("\n").append(
        " : ${
            try {
                responseMessage
            } catch (ex: Exception) {
                "Error"
            }
        }"
    )
}

fun TransactionResponse.buildSMSText(s: String? = null): StringBuilder = StringBuilder().apply {
    append("POS $transactionType ${if (responseCode == "00") "Approved" else "Declined"}\n\n")
    append("Response Code: $responseCode\n")
    append(
        "Message: ${
            try {
                responseMessage
            } catch (e: java.lang.Exception) {
                ""
            }
        }\n"
    )
    append("Amount: ${amount.div(100).formatCurrencyAmount("\u20A6")}\n")
    append("Date/Time: ${transactionTimeInMillis.formatDate()}\n")
    s?.let {
        append("Remark: $it\n")
    }
    append("Auth Code: $authCode\n")
    append("RRN: $RRN\n")
    append("STAN: $STAN\n")
    append("Card: $cardLabel - $maskedPan\n")
    append("Card Owner: $cardHolder\n")
    append("Merchant: ${Singletons.getCurrentlyLoggedInUser()?.business_name}\n")
    append("Terminal ID: $terminalId\n")
}

fun TransactionResponse.buildReceipt(
    context: Context,
    isMerchantCopy: Boolean = false,
    remark: String? = null
) =
    ReceiptBuilder(context).also { builder ->
        builder.appendLogo(BitmapFactory.decodeResource(context.resources, R.drawable.ic_netpos_logo))
        builder.appendAID(AID)
        builder.appendAddress(Singletons.getCurrentlyLoggedInUser()!!.business_name)
        builder.appendAmount(
            amount.div(100).formatCurrencyAmount("\u20A6")
        )
        remark?.let {
            builder.appendRemark(it)
        }
        builder.appendAppName("NetPOS")
        builder.appendAppVersion(BuildConfig.VERSION_NAME)
        builder.appendAuthorizationCode(authCode)
        builder.appendCardHolderName(cardHolder)
        builder.appendCardNumber(maskedPan)
        builder.appendCardScheme(cardLabel)
        builder.appendDateTime(transactionTimeInMillis.formatDate())
        builder.appendRRN(RRN)
        builder.appendStan(STAN)
        builder.appendTerminalId(terminalId)
        builder.appendTransactionType(transactionType.name)
        builder.appendTransactionStatus(if (responseCode == "00") "Approved" else "Declined")
        builder.appendResponseCode(
            "${responseCode}\nMessage: ${
                try {
                    responseMessage
                } catch (ex: Exception) {
                    "Error"
                }
            }"
        )
        if (isMerchantCopy)
            builder.isMerchantCopy
        else builder.isCustomerCopy
    }

fun NipNotification.print(context: Context, printerListener: POIPrinterManage.IPrinterListener) {
    buildNipReceipt(context).print(printerListener)
}

fun NipNotification.print(context: Context): Single<PrinterResponse> {
    return buildNipReceipt(context).print()
}

fun NipNotification.buildNipReceipt(context: Context): ReceiptBuilder =
    ReceiptBuilder(context).apply {
        appendLogo(BitmapFactory.decodeResource(context.resources, R.drawable.ic_netpos_logo))
        appendTextEntityFontSixteenCenter("BANK TRANSFER")
        appendTextEntity("\nBeneficiary Account Number: $beneficiaryAccountNumber")
        appendTextEntity("Source Name: $sourceName")
        appendTextEntity("Source Account Number: $sourceAccountNumber")
        appendTextEntity(
            "Amount: \u20A6${amount}"
        )
        appendTextEntity("Date: $createdAt")
    }


fun List<NipNotification>.printAllNotifications(context: Context): Observable<PrinterResponse> {
    var emitter: ObservableEmitter<PrinterResponse>? = null
    val printerListener = object : POIPrinterManage.IPrinterListener {
        override fun onError(p0: Int, p1: String?) {
            emitter?.let {
                if (it.isDisposed.not())
                    it.onError(Throwable("message:$p1 - code:$p0"))
            }
        }

        override fun onFinish() {
            emitter?.let {
                if (it.isDisposed.not())
                    it.onNext(PrinterResponse())
            }
        }

        override fun onStart() {
            Timber.e("Printing started")
        }
    }
    return Observable.create {
        emitter = it
        forEach { nipNotification ->
            if (it.isDisposed.not())
                nipNotification.print(context, printerListener)
        }
        it.onComplete()
    }
}
