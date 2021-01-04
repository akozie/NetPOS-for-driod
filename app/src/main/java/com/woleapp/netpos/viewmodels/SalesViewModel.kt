package com.woleapp.netpos.viewmodels

import android.content.Context
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.google.gson.JsonObject
import com.netpluspay.kozenlib.KozenLib
import com.netpluspay.kozenlib.printer.PrinterResponse
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.model.*
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.Singletons.getCurrentlyLoggedInUser
import com.woleapp.netpos.util.Singletons.gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.HttpException
import timber.log.Timber


class SalesViewModel : ViewModel() {
    var cardData: CardData? = null
    private val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }
    val transactionState = MutableLiveData(STATE_PAYMENT_STAND_BY)
    private val lastTransactionResponse = MutableLiveData<TransactionResponse>()
    val amount: MutableLiveData<String> = MutableLiveData<String>("")
    private var event: MqttEvent
    private var amountLong = 0L
    var pin = MutableLiveData("")
    val customerName = MutableLiveData("")
    val remark = MutableLiveData("")
    private var isoAccountType: IsoAccountType? = null
    private var cardScheme: String? = null
    private val _showPrintDialog = MutableLiveData<Event<String>>()
    private var amountDbl: Double = 0.0
    private val _shouldRefreshNibssKeys = MutableLiveData<Event<Boolean>>()
    val shouldRefreshNibssKeys: LiveData<Event<Boolean>>
        get() = _shouldRefreshNibssKeys
    private val _finish = MutableLiveData<Event<Boolean>>()
    private val _showPrinterError = MutableLiveData<Event<String>>()

    val showPrinterError: LiveData<Event<String>>
        get() = _showPrinterError

    val finish: LiveData<Event<Boolean>>
        get() = _finish
    private val _message: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }
    private val _smsSent = MutableLiveData<Event<Boolean>>()
    val smsSent: LiveData<Event<Boolean>>
        get() = _smsSent

    private val _toastMessage = MutableLiveData<Event<String>>()
    val toastMessage: LiveData<Event<String>>
        get() = _toastMessage
    private val _getCardData = MutableLiveData<Event<Boolean>>()

    val showPrintDialog: LiveData<Event<String>>
        get() = _showPrintDialog

    val getCardData: LiveData<Event<Boolean>>
        get() = _getCardData

    init {
        val user = getCurrentlyLoggedInUser()
        event = MqttEvent(
            user!!.netplus_id!!,
            user.business_name!!,
            NetPosTerminalConfig.getTerminalId(),
            KozenLib.getDeviceSerial()
        )
    }

    val message: LiveData<Event<String>>
        get() = _message

    fun setCustomerName(name: String) {
        customerName.value = name
    }

    fun validateField() {
        amountDbl = (amount.value!!.toDoubleOrNull() ?: kotlin.run {
            _message.value = Event("Enter a valid amount")
            return
        }) * 100
        _getCardData.value = Event(true)
    }

    fun makePayment(context: Context, transactionType: TransactionType = TransactionType.PURCHASE) {
        Timber.e(cardData.toString())

        val configData = NetPosTerminalConfig.getConfigData() ?: kotlin.run {
            _message.value =
                Event("Terminal has not been configured, restart the application to configure")
            return
        }
        val keyHolder = NetPosTerminalConfig.getKeyHolder()!!
        Timber.e("terminal id for transaction ${NetPosTerminalConfig.getTerminalId()}")
        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.getConnectionData(),
            keyHolder,
            configData
        )
        //IsoAccountType.
        this.amountLong = amountDbl.toLong()
        val requestData =
            TransactionRequestData(transactionType, amountLong, 0L, accountType = isoAccountType!!)
        val processor = TransactionProcessor(hostConfig)
        transactionState.value = STATE_PAYMENT_STARTED
        processor.processTransaction(context, requestData, cardData!!)
            .flatMap {
                if (it.responseCode == "A3")
                    _shouldRefreshNibssKeys.postValue(Event(true))
                event.apply {
                    this.event = MqttEvents.TRANSACTIONS.event
                    this.code = it.responseCode
                    this.timestamp = System.currentTimeMillis()
                    this.data = it.toNibssResponse(remark = remark.value)
                    this.transactionType = transactionType.name
                    this.status = try {
                        it.responseMessage
                    } catch (ex: Exception) {
                        "Error"
                    }
                }
                Timber.e(gson.toJson(event))
                MqttHelper.sendPayload(MqttTopics.TRANSACTIONS, event)
                it.cardHolder = customerName.value!!
                it.cardLabel = cardScheme!!
                lastTransactionResponse.postValue(it)
                Timber.e(it.toString())
                Timber.e(it.responseCode)
                Timber.e(it.responseMessage)
                _message.postValue(Event(if (it.responseCode == "00") "Transaction Approved" else "Transaction Not approved"))
                AppDatabase.getDatabaseInstance(context).transactionResponseDao()
                    .insertNewTransaction(it)
            }
            .flatMap {
                printReceipt()
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                transactionState.value = STATE_PAYMENT_STAND_BY
            }.subscribe { t1, throwable ->
                t1?.let {
                    event.apply {
                        this.event = MqttEvents.PRINTING_RECEIPT.event
                        this.code = it.code.toString()
                        this.timestamp = System.currentTimeMillis()
                        this.data = PrinterEventData(
                            lastTransactionResponse.value!!.RRN,
                            "Printer code name"
                        )
                        this.status = it.message
                    }
                    MqttHelper.sendPayload(MqttTopics.PRINTING_RECEIPT, event)
                    //_message.value = Event("")
                    _finish.value = Event(true)
                }
                throwable?.let {
                    _showPrinterError.value = Event(it.localizedMessage)
                    _message.value = Event("Error: ${it.localizedMessage}")
                    Timber.e(it)
                }
            }.disposeWith(compositeDisposable)
    }


    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    fun setAccountType(accountType: IsoAccountType) {
        this.isoAccountType = accountType
    }

    fun setCardScheme(cardScheme: String?) {
        this.cardScheme = if (cardScheme.equals("no match", true)) "VERVE" else cardScheme
    }

    fun showReceiptDialog() {
        _showPrintDialog.value = Event(lastTransactionResponse.value!!.buildSMSText(remark.value ?: "")
            .toString())
    }

    private fun printReceipt(): Single<PrinterResponse> {
        val transactionResponse = lastTransactionResponse.value!!
        return if (Build.MODEL == "P3") transactionResponse.print(remark.value ?: "")
            .subscribeOn(Schedulers.io()) else {
            _showPrintDialog.postValue(
                Event(
                    transactionResponse.buildSMSText(remark.value ?: "").toString()
                )
            )
            Single.just(PrinterResponse())
        }
    }

    fun sendSmS(number: String) {
        val map = JsonObject().apply {
            addProperty("from", "NetPlus")
            addProperty("to", "+234${number.substring(1)}")
            addProperty("message", lastTransactionResponse.value!!.buildSMSText(remark.value ?: "").toString())
        }
        Timber.e("payload: $map")
        val auth = "Bearer ${Prefs.getString(PREF_APP_TOKEN, "")}"
        val body: RequestBody = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            map.toString()
        )
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
}