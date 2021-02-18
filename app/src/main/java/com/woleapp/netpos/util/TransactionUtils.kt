package com.woleapp.netpos.util

import android.content.Context
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

fun reverseTransaction(
    context: Context,
    transactionResponse: TransactionResponse,
    cardData: CardData,
    accountType: IsoAccountType,
    disposable: CompositeDisposable,
    fn: (transactionResponse: TransactionResponse?, error: Throwable?) -> Unit
) {
    val originalDataElements = transactionResponse.toOriginalDataElements()
    val hostConfig = HostConfig(
        NetPosTerminalConfig.getTerminalId(),
        NetPosTerminalConfig.getConnectionData(),
        NetPosTerminalConfig.getKeyHolder()!!,
        NetPosTerminalConfig.getConfigData()!!
    )

    val requestData = TransactionRequestData(
        transactionType = TransactionType.REVERSAL,
        amount = originalDataElements.originalAmount,
        originalDataElements = originalDataElements,
        accountType = accountType
    )
    TransactionProcessor(hostConfig).processTransaction(
        context,
        requestData,
        cardData
    ).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { response, error ->
            fn.invoke(response, error)
        }.disposeWith(disposable)
}

fun insertNewTransaction(
    appDatabase: AppDatabase,
    transactionResponse: TransactionResponse,
    compositeDisposable: CompositeDisposable,
    fn: (l: Long?, error: Throwable?) -> Unit
) {
    appDatabase.transactionResponseDao().insertNewTransaction(transactionResponse)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { t1, t2 ->
            fn.invoke(t1, t2)
        }.disposeWith(compositeDisposable)

}

fun updateTransaction(
    appDatabase: AppDatabase,
    transactionResponse: TransactionResponse,
    compositeDisposable: CompositeDisposable,
    fn: (i: Int?, error: Throwable?) -> Unit
) {
    appDatabase.transactionResponseDao().updateTransaction(transactionResponse)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { t1, t2 ->
            fn.invoke(t1, t2)
        }.disposeWith(compositeDisposable)
}


fun pay(
    context: Context,
    amount: Long,
    otherAmount: Long,
    cardData: CardData,
    transactionType: TransactionType,
    accountType: IsoAccountType,
    compositeDisposable: CompositeDisposable,
    fn: (transactionResponse: TransactionResponse?, error: Throwable?) -> Unit
) {
    val hostConfig = HostConfig(
        NetPosTerminalConfig.getTerminalId(),
        NetPosTerminalConfig.getConnectionData(),
        NetPosTerminalConfig.getKeyHolder()!!,
        NetPosTerminalConfig.getConfigData()!!
    )
    val requestData =
        TransactionRequestData(transactionType, amount, otherAmount, accountType = accountType)
    val processor = TransactionProcessor(hostConfig)
    processor.processTransaction(context, requestData, cardData)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { t1, t2 ->
            fn.invoke(t1, t2)
        }.disposeWith(compositeDisposable)
}

