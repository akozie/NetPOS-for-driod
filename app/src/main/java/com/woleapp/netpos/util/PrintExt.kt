package com.woleapp.netpos.util

import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.responseMessage
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.netpluspay.kozenlib.printer.PrinterResponse
import com.netpluspay.kozenlib.printer.ReceiptBuilder
import com.pos.sdk.printer.POIPrinterManage
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.model.NipNotification
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import timber.log.Timber

fun List<TransactionResponse>.printAll(isMerchantCopy: Boolean): Observable<PrinterResponse> {
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
                transactionResponse.print(printerListener, isMerchantCopy)
        }
        it.onComplete()
    }
}

fun TransactionResponse.print(
    printerListener: POIPrinterManage.IPrinterListener,
    isMerchantCopy: Boolean = true
) {
    buildReceipt(isMerchantCopy).print(printerListener)
}

fun TransactionResponse.print() = buildReceipt().print()

fun TransactionResponse.buildReceipt(isMerchantCopy: Boolean = false) =
    ReceiptBuilder().also { builder ->
        builder.appendLogo()
        builder.appendAID(AID)
        builder.appendAddress("NETPOS")
        builder.appendAmount(
            amount.div(100).formatCurrencyAmount("\u20A6")
        )
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

fun NipNotification.print(printerListener: POIPrinterManage.IPrinterListener) {
    buildNipReceipt.print(printerListener)
}

fun NipNotification.print(): Single<PrinterResponse> {
    return buildNipReceipt.print()
}

val NipNotification.buildNipReceipt: ReceiptBuilder
    get() = ReceiptBuilder().apply {
        appendLogo()
        appendTextEntityFontSixteenCenter("BANK TRANSFER")
        appendTextEntity("\nBeneficiary Account Number: $beneficiaryAccountNumber")
        appendTextEntity("Source Name: $sourceName")
        appendTextEntity("Source Account Number: $sourceAccountNumber")
        appendTextEntity(
            "Amount: \u20A6${amount}"
        )
        appendTextEntity("Date: $createdAt")
    }


fun List<NipNotification>.printAllNotifications(): Observable<PrinterResponse> {
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
                nipNotification.print(printerListener)
        }
        it.onComplete()
    }
}
