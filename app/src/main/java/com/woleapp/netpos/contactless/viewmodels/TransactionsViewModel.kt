package com.woleapp.netpos.contactless.viewmodels

import android.content.Context
import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.danbamitale.epmslib.entities.* // ktlint-disable no-wildcard-imports
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.woleapp.netpos.contactless.database.AppDatabase
import com.woleapp.netpos.contactless.nibss.NetPosTerminalConfig
import com.woleapp.netpos.contactless.util.* // ktlint-disable no-wildcard-imports
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class TransactionsViewModel : ViewModel() {
    private lateinit var endOfDayList: List<TransactionResponse>
    var cardData: CardData? = null
    private val compositeDisposable = CompositeDisposable()
    private var appDatabase: AppDatabase? = null
    val lastTransactionResponse = MutableLiveData<TransactionResponse>()
    private val _selectedAction = MutableLiveData<String>()
    val inProgress = MutableLiveData(false)
    private val _done = MutableLiveData(false)
    private val _beginGetCardDetails = MutableLiveData<Event<Boolean>>()
    private var accountType: IsoAccountType = IsoAccountType.DEFAULT_UNSPECIFIED
    private lateinit var cardHolderName: String
    private val _message = MutableLiveData<Event<String>>()
    private var cardScheme: String? = null
    private val _showProgressDialog = MutableLiveData<Event<Boolean>>()
    private val _showPrintDialog = MutableLiveData<Event<String>>()
    private var transactionType: TransactionType = TransactionType.REFUND

    private val _showPrinterError = MutableLiveData<Event<String>>()

    val showPrinterError: LiveData<Event<String>>
        get() = _showPrinterError

    private val _showReceiptTypeMutableLiveData = MutableLiveData<Event<Boolean>>()

    val showReceiptType: LiveData<Event<Boolean>>
        get() = _showReceiptTypeMutableLiveData

    private val _shouldRefreshNibssKeys = MutableLiveData<Event<Boolean>>()
    val shouldRefreshNibssKeys: LiveData<Event<Boolean>>
        get() = _shouldRefreshNibssKeys

    private val _toastMessage = MutableLiveData<Event<String>>()
    val toastMessage: LiveData<Event<String>>
        get() = _toastMessage

    val showPrintDialog: LiveData<Event<String>>
        get() = _showPrintDialog

    val showProgressDialog: LiveData<Event<Boolean>>
        get() = _showProgressDialog

    val message: LiveData<Event<String>>
        get() = _message
    val beginGetCardDetails: LiveData<Event<Boolean>>
        get() = _beginGetCardDetails

    val done: LiveData<Boolean>
        get() = _done
    val selectedAction: LiveData<String>
        get() = _selectedAction

    var transactionList: List<TransactionResponse>? = null

    fun setSelectedTransaction(transactionResponse: TransactionResponse) {
//        Timber.e(gson.toJson(transactionResponse))
//        Timber.e(gson.toJson(transactionResponse.toNibssResponse()))
        lastTransactionResponse.value = transactionResponse
    }

    fun setAppDatabase(appDatabase: AppDatabase) {
        this.appDatabase = appDatabase
    }

    fun getTransactions() =
        when (_selectedAction.value) {
            HISTORY_ACTION_PREAUTH -> appDatabase!!.transactionResponseDao()
                .getTransactionByTransactionType(TransactionType.PRE_AUTHORIZATION)
            HISTORY_ACTION_REFUND, HISTORY_ACTION_REVERSAL -> appDatabase!!.transactionResponseDao()
                .getRefundableTransactions()
            else -> appDatabase!!.transactionResponseDao()
                .getTransactions(NetPosTerminalConfig.getTerminalId())
        }.map {
            transactionList = it
            it
        }

    fun setAction(action: String?) {
        _selectedAction.value = action!!
    }

    fun performAction() {
        when (_selectedAction.value) {
            HISTORY_ACTION_REPRINT -> printReceipt()
            HISTORY_ACTION_REFUND -> {
                transactionType = TransactionType.REFUND
                _beginGetCardDetails.value = Event(true)
            }
            HISTORY_ACTION_REVERSAL -> {
                transactionType = TransactionType.REVERSAL
                _beginGetCardDetails.value = Event(true)
            }
        }
    }

    fun refundTransaction(context: Context) {
        refundTransaction(lastTransactionResponse.value!!, context)
    }

    fun reset() {
        _done.value = false
    }

    private fun refundTransaction(transactionResponse: TransactionResponse, context: Context) {
        val originalDataElements = transactionResponse.toOriginalDataElements()

        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.connectionData,
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!
        )

        val requestData = TransactionRequestData(
            transactionType = transactionType,
            amount = originalDataElements.originalAmount,
            originalDataElements = originalDataElements,
            accountType = accountType
        )
        inProgress.value = true
        TransactionProcessor(hostConfig).processTransaction(
            context,
            requestData,
            cardData!!
        ).flatMap {
            if (it.responseCode == "A3") {
                _shouldRefreshNibssKeys.postValue(Event(true))
            }
            _message.postValue(Event("Transaction: ${it.responseMessage}"))
            it.cardHolder = cardHolderName
            it.cardLabel = cardScheme!!
            it.id = transactionResponse.id
            lastTransactionResponse.postValue(it)
            appDatabase!!.transactionResponseDao().updateTransaction(it)
        }
            .doFinally {
                inProgress.postValue(false)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    _message.value = Event(it.localizedMessage ?: "")
                    Timber.e(it)
                    it.printStackTrace()
                }

                response?.let {
                    printReceipt()
                }
            }.disposeWith(compositeDisposable)
    }

    private fun printReceipt() {
        val transactionResponse = lastTransactionResponse.value!!
            .apply {
                this.cardExpiry = ""
            }
        _showPrintDialog.postValue(
            Event(transactionResponse.buildSMSText().toString())
        )
    }

    fun showReceiptDialog() {
        _showPrintDialog.value = Event(
            lastTransactionResponse.value!!.buildSMSText()
                .toString()
        )
    }

    fun setCustomerName(cardHolderName: String) {
        this.cardHolderName = cardHolderName
    }

    fun setAccountType(accountType: IsoAccountType) {
        this.accountType = accountType
    }

    fun setCardScheme(cardScheme: String?) {
        this.cardScheme = if (cardScheme.equals("no match", true)) "VERVE" else cardScheme
    }

    fun doSaleCompletion(context: Context) {
        val transactionResponse = lastTransactionResponse.value!!
        val originalDataElements = transactionResponse.toOriginalDataElements()
        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.connectionData,
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!
        )
        // 0428084454
        // 0428084454

        val requestData = TransactionRequestData(
            transactionType = TransactionType.PRE_AUTHORIZATION_COMPLETION,
            amount = originalDataElements.originalAmount,
            originalDataElements = originalDataElements
        )

        _showProgressDialog.value = Event(true)
        TransactionProcessor(hostConfig).processTransaction(
            context,
            requestData,
            cardData!!
        ).flatMap {
            if (it.responseCode == "A3") {
                _shouldRefreshNibssKeys.postValue(Event(true))
            }
            _showProgressDialog.postValue(Event(false))
            _message.postValue(Event("Transaction: ${it.responseMessage}"))
            it.cardHolder = cardHolderName
            it.cardLabel = cardScheme!!
            it.id = transactionResponse.id
            lastTransactionResponse.postValue(it)
            appDatabase!!.transactionResponseDao().updateTransaction(it)
        }.doFinally {
            inProgress.postValue(false)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    _message.value = Event(it.localizedMessage ?: "")
                    Timber.e(it)
                    it.printStackTrace()
                }

                response?.let {
                    printReceipt()
                }
            }.disposeWith(compositeDisposable)
    }

    fun preAuthRefund(context: Context) {
        val transactionResponse = lastTransactionResponse.value!!
        val originalDataElements = transactionResponse.toOriginalDataElements()

        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.connectionData,
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!
        )

        val requestData = TransactionRequestData(
            transactionType = TransactionType.REFUND,
            amount = originalDataElements.originalAmount,
            originalDataElements = originalDataElements
        )
        _showProgressDialog.value = Event(true)
        TransactionProcessor(hostConfig).processTransaction(context, requestData, cardData!!)
            .flatMap {
                if (it.responseCode == "A3") {
                    _shouldRefreshNibssKeys.postValue(Event(true))
                }
                _showProgressDialog.postValue(Event(false))
                _message.postValue(Event("Transaction: ${it.responseMessage}"))
                it.cardHolder = cardHolderName
                it.cardLabel = cardScheme!!
                it.id = transactionResponse.id
                lastTransactionResponse.postValue(it)
                appDatabase!!.transactionResponseDao().updateTransaction(it)
            }.doFinally {
                inProgress.postValue(false)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    _message.value = Event(it.localizedMessage ?: "")
                    Timber.e(it)
                    it.printStackTrace()
                }

                response?.let {
                    printReceipt()
                }
            }.disposeWith(compositeDisposable)
    }

    fun setEndOfDayList(eodList: List<TransactionResponse>) {
        this.endOfDayList = eodList
    }

    fun getEodList() = endOfDayList
}
