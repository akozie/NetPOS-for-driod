package com.woleapp.netpos.viewmodels

import android.content.Context
import androidx.lifecycle.*
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.netplus.sunyardlib.ReceiptBuilder
import com.socsi.smartposapi.printer.PrintRespCode
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.nibss.NetPosTerminalConfig.Companion.getConnectionData
import com.woleapp.netpos.nibss.NetPosTerminalConfig.Companion.getTerminalId
import com.woleapp.netpos.util.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class TransactionsViewModel : ViewModel() {
    var cardData: CardData? = null
    private val compositeDisposable = CompositeDisposable()
    private var appDatabase: AppDatabase? = null
    val selectedTransaction = MutableLiveData<TransactionResponse>()
    private val lastTransactionResponse = MutableLiveData<TransactionResponse>()
    private val _selectedAction = MutableLiveData<String>()
    val inProgress = MutableLiveData(false)
    private val _done = MutableLiveData(false)
    private val _beginGetCardDetails = MutableLiveData<Event<Boolean>>()

    val beginGetCardDetails: LiveData<Event<Boolean>>
        get() = _beginGetCardDetails

    val done: LiveData<Boolean>
        get() = _done
    val selectedAction: LiveData<String>
        get() = _selectedAction


    fun setSelectedTransaction(transactionResponse: TransactionResponse) {
        selectedTransaction.value = transactionResponse
    }

    fun setAppDatabase(appDatabase: AppDatabase) {
        this.appDatabase = appDatabase
    }

    fun getTransactions() = appDatabase!!.transactionResponseDao().getTransactions()
    fun setAction(action: String?) {
        _selectedAction.value = action
    }

    fun performAction() {
        when (_selectedAction.value) {
            HISTORY_ACTION_REPRINT -> startPrintingReceipt(selectedTransaction.value!!)
            HISTORY_ACTION_REFUND -> {
                _beginGetCardDetails.value = Event(true)
            }
        }
    }

    fun refundTransaction(context: Context){
        refundTransaction(selectedTransaction.value!!, context)
    }

    fun reset() {
        _done.value = false
    }

    private fun refundTransaction(transactionResponse: TransactionResponse, context: Context) {
        val originalDataElements = transactionResponse.toOriginalDataElements()

        val hostConfig = HostConfig(
            getTerminalId(),
            getConnectionData(),
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!
        )

        val requestData = TransactionRequestData(
            transactionType = TransactionType.REVERSAL,
            amount = originalDataElements.originalAmount,
            originalDataElements = originalDataElements
        )
        inProgress.value = true
        val disposable = TransactionProcessor(hostConfig).processTransaction(
            context,
            requestData,
            cardData!!
        ).flatMap {
            if (it.responseCode != "00")
                throw Exception("Transaction Failed")
            it.id = transactionResponse.id
            lastTransactionResponse.postValue(it)
            appDatabase!!.transactionResponseDao().updateTransaction(it)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    inProgress.value = false
                    Timber.e(it)
                    it.printStackTrace()
                }

                response?.let {
                    startPrintingReceipt(lastTransactionResponse.value!!)
                }
            }
        compositeDisposable.add(disposable)
    }

    private fun startPrintingReceipt(transactionResponse: TransactionResponse) {
        Timber.e(transactionResponse.toString())
        inProgress.value = true
        printReceipt(transactionResponse)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { _, t2 ->
                _done.value = true
                inProgress.value = false

                t2?.let {
                    Timber.e(it)
                }
            }.disposeWith(compositeDisposable)
    }

    private fun printReceipt(transactionResponse: TransactionResponse): Single<PrintRespCode> =
        ReceiptBuilder()
            .apply {
                appendAID(transactionResponse.AID)
                appendAddress("NETPOS")
                appendAmount(transactionResponse.amount.div(100).toString())
                appendAppName("NetPOS")
                appendAppVersion(BuildConfig.VERSION_NAME)
                appendAuthorizationCode(transactionResponse.authCode)
                appendCardHolderName(transactionResponse.cardHolder)
                appendCardNumber(transactionResponse.maskedPan)
                appendCardScheme("card scheme")
                appendDateTime(transactionResponse.transactionTimeInMillis.formatDate())
                appendRRN(transactionResponse.RRN)
                appendStan(transactionResponse.STAN)
                appendTerminalId(getTerminalId())
                appendTransactionType(transactionResponse.transactionType.name)
                appendTransactionStatus(if (transactionResponse.responseCode == "00") "Approved" else "Declined")
                appendResponseCode(transactionResponse.responseCode)
            }.print()


}