package com.woleapp.netpos.viewmodels

import android.content.Context
import android.os.Build
import androidx.lifecycle.*
import com.google.gson.JsonObject
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.printer.PrinterResponse
import com.netpluspay.nibssclient.models.*
import com.netpluspay.nibssclient.service.NibssApiWrapper
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.model.*
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig.Companion.getTerminalId
import com.woleapp.netpos.util.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import timber.log.Timber

class TransactionsViewModel : ViewModel() {
    private lateinit var endOfDayList: List<TransactionResponse>
    var cardData: CardData? = null
    private lateinit var context: Context
    private val compositeDisposable = CompositeDisposable()
    private var appDatabase: AppDatabase? = null
    val lastTransactionResponse = MutableLiveData<TransactionResponse>()
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

    private val _showPrinterError = MutableLiveData<Event<String>>()

    val showPrinterError: LiveData<Event<String>>
        get() = _showPrinterError


    private val _shouldRefreshNibssKeys = MutableLiveData<Event<Boolean>>()
    val shouldRefreshNibssKeys: LiveData<Event<Boolean>>
        get() = _shouldRefreshNibssKeys

    private val _smsSent = MutableLiveData<Event<Boolean>>()
    val smsSent: LiveData<Event<Boolean>>
        get() = _smsSent

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


    init {
        val user = Singletons.getCurrentlyLoggedInUser()
        event = MqttEvent(
            user!!.netplus_id!!,
            user.business_name!!,
            getTerminalId(),
            NetPosSdk.getDeviceSerial()
        )
    }

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
            HISTORY_ACTION_REFUND -> appDatabase!!.transactionResponseDao()
                .getRefundableTransactions()
            else -> appDatabase!!.transactionResponseDao().getTransactions()
        }


    fun setAction(action: String?) {
        _selectedAction.value = action!!
    }

    fun performAction() {
        when (_selectedAction.value) {
            HISTORY_ACTION_REPRINT -> startPrintingReceipt(lastTransactionResponse.value!!)
            HISTORY_ACTION_REFUND -> {
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
        val refundTransactionParams = RefundTransactionParams(cardData!!, transactionResponse, accountType, MessageReasonCode.CompletedPartially)
        inProgress.value = true
        NibssApiWrapper.refundTransaction(context, refundTransactionParams).flatMap {
            if (it.responseCode == "A3")
                _shouldRefreshNibssKeys.postValue(Event(true))
            _message.postValue(Event("Transaction: ${it.responseMessage}"))
            it.cardHolder = cardHolderName
            it.cardLabel = cardScheme!!
            event.apply {
                this.event = MqttEvents.TRANSACTIONS.event
                this.code = it.responseCode
                this.timestamp = System.currentTimeMillis()
                this.data = it.toNibssResponse()
                this.transactionType = it.transactionType.name
                this.status = try {
                    it.responseMessage
                } catch (ex: Exception) {
                    "Error"
                }
            }
            MqttHelper.sendPayload(MqttTopics.TRANSACTIONS, event)
            it.id = transactionResponse.id
            lastTransactionResponse.postValue(it)
            appDatabase!!.transactionResponseDao().updateTransaction(it)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    inProgress.value = false
                    _message.value = Event(it.localizedMessage ?: "Error")
                    Timber.e(it)
                    it.printStackTrace()
                }

                response?.let {
                    startPrintingReceipt(lastTransactionResponse.value!!)
                }
            }.disposeWith(compositeDisposable)

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
                    MqttHelper.sendPayload(MqttTopics.PRINTING_RECEIPT, event)
                }
                _done.value = true
                inProgress.value = false

                t2?.let {
                    _showPrinterError.value = Event(it.localizedMessage ?: "Error")
                    Timber.e(it)
                    _message.value = Event(it.localizedMessage ?: "Error")
                }
            }.disposeWith(compositeDisposable)
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

        val makePaymentParams = MakePaymentParams(amount = originalDataElements.originalAmount, transactionType = TransactionType.PRE_AUTHORIZATION_COMPLETION).apply {
            this.originalDataElements = originalDataElements
        }

        _showProgressDialog.value = Event(true)
        NibssApiWrapper.completion(context, makePaymentParams).flatMap {
            if (it.responseCode == "A3")
                _shouldRefreshNibssKeys.postValue(Event(true))
            _showProgressDialog.postValue(Event(false))
            _message.postValue(Event("Transaction: ${it.responseMessage}"))
            it.cardHolder = cardHolderName
            it.cardLabel = cardScheme!!
            event.apply {
                this.event = MqttEvents.TRANSACTIONS.event
                this.code = it.responseCode
                this.timestamp = System.currentTimeMillis()
                this.data = it.toNibssResponse()
                this.transactionType = it.transactionType.name
                this.status = try {
                    it.responseMessage
                } catch (ex: Exception) {
                    "Error"
                }
            }
            MqttHelper.sendPayload(MqttTopics.TRANSACTIONS, event)
            it.id = transactionResponse.id
            lastTransactionResponse.postValue(it)
            appDatabase!!.transactionResponseDao().updateTransaction(it)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    _message.value = Event(it.localizedMessage ?: "Error")
                    Timber.e(it)
                    it.printStackTrace()
                }

                response?.let {
                    startPrintingReceipt(lastTransactionResponse.value!!)
                }
            }.disposeWith(compositeDisposable)
    }

    fun preAuthRefund(context: Context) {
        val transactionResponse = lastTransactionResponse.value!!
        val refundTransactionParams = RefundTransactionParams(cardData!!, transactionResponse, accountType)
        _showProgressDialog.value = Event(true)
        NibssApiWrapper.refundTransaction(context, refundTransactionParams)
            .flatMap {
                if (it.responseCode == "A3")
                    _shouldRefreshNibssKeys.postValue(Event(true))
                _showProgressDialog.postValue(Event(false))
                _message.postValue(Event("Transaction: ${it.responseMessage}"))
                it.cardHolder = cardHolderName
                it.cardLabel = cardScheme!!
                event.apply {
                    this.event = MqttEvents.TRANSACTIONS.event
                    this.code = it.responseCode
                    this.timestamp = System.currentTimeMillis()
                    this.data = it.toNibssResponse()
                    this.transactionType = it.transactionType.name
                    this.status = try {
                        it.responseMessage
                    } catch (ex: Exception) {
                        "Error"
                    }
                }
                MqttHelper.sendPayload(MqttTopics.TRANSACTIONS, event)
                it.id = transactionResponse.id
                lastTransactionResponse.postValue(it)
                appDatabase!!.transactionResponseDao().updateTransaction(it)
            }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    _message.value = Event(it.localizedMessage ?: "Error")
                    Timber.e(it)
                    it.printStackTrace()
                }

                response?.let {
                    startPrintingReceipt(lastTransactionResponse.value!!)
                }
            }.disposeWith(compositeDisposable)
    }

    private fun printReceipt(transactionResponse: TransactionResponse): Single<PrinterResponse> {
        return if (Build.MODEL == "P3") transactionResponse.print(context)
        else {
            _showPrintDialog.postValue(Event(transactionResponse.buildSMSText().toString()))
            Single.just(PrinterResponse())
        }
    }

    fun sendSmS(number: String) {
        val map = JsonObject().apply {
            addProperty("from", "NetPlus")
            addProperty("to", "+234${number.substring(1)}")
            addProperty("message", lastTransactionResponse.value!!.buildSMSText().toString())
        }
        Timber.e("payload: $map")
        val auth = "Bearer ${Prefs.getString(PREF_APP_TOKEN, "")}"
        val body: RequestBody = map.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val smsEvent = event

        StormApiClient.getSmsServiceInstance().sendSms(auth, body)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    _smsSent.value = Event(true)
                    smsEvent.apply {
                        event = MqttEvents.SMS_EVENTS.event
                        status = "SUCCESS"
                        code = "200"
                        data = SMSEvent("+234${number.substring(1)}", "Success", it.toString())
                    }
                    MqttHelper.sendPayload(MqttTopics.SMS_EVENTS, smsEvent)
                    Timber.e("Data $it")
                }
                t2?.let {
                    smsEvent.apply {
                        event = MqttEvents.SMS_EVENTS.event
                        status = "ERROR"
                        code = "-99"
                        data = SMSEvent(
                            "+234${number.substring(1)}",
                            "Failed",
                            it.localizedMessage ?: "Error"
                        )
                    }
                    val httpException = it as? HttpException
                    httpException?.let { e ->
                        smsEvent.code = e.code().toString()
                        smsEvent.data.let { data ->
                            e.response()?.errorBody()?.string()?.let { serverError ->
                                (data as SMSEvent).serverResponse = serverError
                            }
                        }
                    }
                    MqttHelper.sendPayload(MqttTopics.SMS_EVENTS, smsEvent)
                    _smsSent.value = Event(false)
                    _toastMessage.value = Event("Error: ${it.localizedMessage}")
                }
            }.disposeWith(compositeDisposable)
    }

    fun setEndOfDayList(eodList: List<TransactionResponse>) {
        this.endOfDayList = eodList;
    }

    fun getEodList() = endOfDayList

    fun setContext(context: Context){
        this.context = context
    }
}