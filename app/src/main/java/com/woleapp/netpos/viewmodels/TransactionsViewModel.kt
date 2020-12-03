package com.woleapp.netpos.viewmodels

import android.content.Context
import android.os.Build
import androidx.lifecycle.*
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.netpluspay.kozenlib.KozenLib
import com.netpluspay.kozenlib.printer.PrinterResponse
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.model.CardReaderMqttEvent
import com.woleapp.netpos.model.MqttEvent
import com.woleapp.netpos.model.MqttEvents
import com.woleapp.netpos.model.PrinterEventData
import com.woleapp.netpos.mqtt.MqttHelper
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
    private var event: MqttEvent
    private var accountType: IsoAccountType = IsoAccountType.DEFAULT_UNSPECIFIED
    private lateinit var cardHolderName: String
    private val _message = MutableLiveData<Event<String>>()
    private var cardScheme: String? = null
    private val _showProgressDialog = MutableLiveData<Event<Boolean>>()
    private val _showPrintDialog = MutableLiveData<Event<String>>()

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


    init {
        val user = Singletons.getCurrentlyLoggedInUser()
        event = MqttEvent(
            user!!.netplus_id!!,
            user.business_name!!,
            getTerminalId(),
            KozenLib.getDeviceSerial()
        )
    }

    fun setSelectedTransaction(transactionResponse: TransactionResponse) {
        selectedTransaction.value = transactionResponse
    }

    fun setAppDatabase(appDatabase: AppDatabase) {
        this.appDatabase = appDatabase
    }

    fun getTransactions() =
        when (_selectedAction.value) {
            HISTORY_ACTION_PREAUTH -> appDatabase!!.transactionResponseDao()
                .getTransactionByTransactionType(TransactionType.PRE_AUTHORIZATION)
            HISTORY_ACTION_REFUND -> appDatabase!!.transactionResponseDao()
                .getRefundableTransactions()
            else -> appDatabase!!.transactionResponseDao().getTransactions()
        }


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

    fun refundTransaction(context: Context) {
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
            originalDataElements = originalDataElements,
            accountType = accountType
        )
        inProgress.value = true
        val disposable = TransactionProcessor(hostConfig).processTransaction(
            context,
            requestData,
            cardData!!
        ).flatMap {
            _message.postValue(Event("Transaction: ${it.responseMessage}"))
            it.cardHolder = cardHolderName
            it.cardLabel = cardScheme!!
            event.apply {
                this.event = MqttEvents.TRANSACTIONS.event
                this.code = it.responseCode
                this.timestamp = System.currentTimeMillis()
                this.data = it
                this.transactionType = it.transactionType.name
                this.status = try {
                    it.responseMessage
                } catch (ex: Exception) {
                    "Error"
                }
            }
            MqttHelper.sendPayload(event)
            it.id = transactionResponse.id
            lastTransactionResponse.postValue(it)
            appDatabase!!.transactionResponseDao().updateTransaction(it)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    inProgress.value = false
                    _message.value = Event(it.localizedMessage)
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
            .subscribe { t1, t2 ->
                t1?.let {
                    event.apply {
                        this.event = MqttEvents.PRINTING_RECEIPT.event
                        this.code = it.code.toString()
                        this.timestamp = System.currentTimeMillis()
                        this.data = PrinterEventData(transactionResponse.RRN, it.message)
                        this.status = it.message
                    }
                    MqttHelper.sendPayload(event)
                }
                _done.value = true
                inProgress.value = false

                t2?.let {
                    Timber.e(it)
                    _message.value = Event(it.localizedMessage)
                }
            }.disposeWith(compositeDisposable)
    }

    private fun printReceipt(transactionResponse: TransactionResponse): Single<PrinterResponse> {
        if (Build.MODEL != "P3") _showPrintDialog.postValue(Event(transactionResponse.builder().toString()))
        return if (Build.MODEL == "P3") transactionResponse.print()
        else Single.just(PrinterResponse())
    }


    fun sendCardEvent(status: String, code: String, eventData: CardReaderMqttEvent) {
        event.apply {
            data = eventData
            this.status = status
            timestamp = System.currentTimeMillis()
            this.code = code
        }
        MqttHelper.sendPayload(event)
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
        val transactionResponse = selectedTransaction.value!!
        val originalDataElements = transactionResponse.toOriginalDataElements()
        val hostConfig = HostConfig(
            getTerminalId(),
            getConnectionData(),
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!
        )

        val requestData = TransactionRequestData(
            transactionType = TransactionType.PRE_AUTHORIZATION_COMPLETION,
            amount = originalDataElements.originalAmount,
            originalDataElements = originalDataElements
        )

        _showProgressDialog.value = Event(true)
        val disposable = TransactionProcessor(hostConfig).processTransaction(
            context, requestData,
            cardData!!
        ).flatMap {
            _showProgressDialog.postValue(Event(false))
            _message.postValue(Event("Transaction: ${it.responseMessage}"))
            it.cardHolder = cardHolderName
            it.cardLabel = cardScheme!!
            event.apply {
                this.event = MqttEvents.TRANSACTIONS.event
                this.code = it.responseCode
                this.timestamp = System.currentTimeMillis()
                this.data = it
                this.transactionType = it.transactionType.name
                this.status = try {
                    it.responseMessage
                } catch (ex: Exception) {
                    "Error"
                }
            }
            MqttHelper.sendPayload(event)
            it.id = transactionResponse.id
            lastTransactionResponse.postValue(it)
            appDatabase!!.transactionResponseDao().updateTransaction(it)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    _message.value = Event(it.localizedMessage)
                    Timber.e(it)
                    it.printStackTrace()
                }

                response?.let {
                    startPrintingReceipt(lastTransactionResponse.value!!)
                }
            }

        compositeDisposable.add(disposable)
    }

    fun preAuthRefund(context: Context) {
        val transactionResponse = selectedTransaction.value!!
        val originalDataElements = transactionResponse.toOriginalDataElements()

        val hostConfig = HostConfig(
            getTerminalId(),
            getConnectionData(),
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!
        )

        val requestData = TransactionRequestData(
            transactionType = TransactionType.REFUND,
            amount = originalDataElements.originalAmount,
            originalDataElements = originalDataElements
        )
        _showProgressDialog.value = Event(true)
        val disposable =
            TransactionProcessor(hostConfig).processTransaction(context, requestData, cardData!!)
                .flatMap {
                    _showProgressDialog.postValue(Event(false))
                    _message.postValue(Event("Transaction: ${it.responseMessage}"))
                    it.cardHolder = cardHolderName
                    it.cardLabel = cardScheme!!
                    event.apply {
                        this.event = MqttEvents.TRANSACTIONS.event
                        this.code = it.responseCode
                        this.timestamp = System.currentTimeMillis()
                        this.data = it
                        this.transactionType = it.transactionType.name
                        this.status = try {
                            it.responseMessage
                        } catch (ex: Exception) {
                            "Error"
                        }
                    }
                    MqttHelper.sendPayload(event)
                    it.id = transactionResponse.id
                    lastTransactionResponse.postValue(it)
                    appDatabase!!.transactionResponseDao().updateTransaction(it)
                }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { response, error ->
                    error?.let {
                        _message.value = Event(it.localizedMessage)
                        Timber.e(it)
                        it.printStackTrace()
                    }

                    response?.let {
                        startPrintingReceipt(lastTransactionResponse.value!!)
                    }
                }

        compositeDisposable.add(disposable)
    }

}