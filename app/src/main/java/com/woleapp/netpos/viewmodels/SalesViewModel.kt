package com.woleapp.netpos.viewmodels

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.errors.POSPrinterException
import com.netpluspay.netpossdk.printer.PrinterResponse
import com.netpluspay.nibssclient.exception.NibssClientException
import com.netpluspay.nibssclient.models.*
import com.netpluspay.nibssclient.service.NibssApiWrapper
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.model.*
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException


class SalesViewModel : ViewModel() {
    private var isVend: Boolean = false
    var cardData: CardData? = null
    private val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }
    val transactionState = MutableLiveData(STATE_PAYMENT_STAND_BY)
    private val lastTransactionResponse = MutableLiveData<TransactionResponse>()
    val amount: MutableLiveData<String> = MutableLiveData<String>("")
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
        Timber.e("terminal id for transaction ${NetPosTerminalConfig.getTerminalId()}")
        //IsoAccountType.
        this.amountLong = amountDbl.toLong()
        Timber.e(transactionType.toString())
        val requestData = MakePaymentParams(
            amountLong,
            0L,
            cardData!!,
            transactionType,
            isoAccountType ?: IsoAccountType.DEFAULT_UNSPECIFIED
        )
        transactionState.value = STATE_PAYMENT_STARTED
        NibssApiWrapper.makePayment(context, requestData)
            .flatMap {
                if (it.responseCode == "A3") {
                    Prefs.remove(PREF_CONFIG_DATA)
                    Prefs.remove(PREF_KEYHOLDER)
                    _shouldRefreshNibssKeys.postValue(Event(true))
                }
                if (isVend) {
                    val j = JsonObject().apply {
                        addProperty("amount", it.amount)
                        addProperty("responseCode", it.responseCode)
                        addProperty("serial_number", NetPosSdk.getDeviceSerial())
                    }
                    sendVendResponse(context, j.toString())
                }
                val transactionEvent = MqttEvent<NibssResponse>()
                transactionEvent.apply {
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
                //Timber.e(gson.toJson(transactionEvent))
                //MqttHelper.sendPayload(MqttTopics.TRANSACTIONS, event)
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
                printReceipt(context)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                Timber.e("error thrown")
                Timber.e(it.localizedMessage)
            }
            .doFinally {
                transactionState.value = STATE_PAYMENT_STAND_BY
            }.subscribe { t1, throwable ->
                val printerEvent = MqttEvent<PrinterEventData>()
                t1?.let {
                    printerEvent.apply {
                        this.event = MqttEvents.PRINTING_RECEIPT.event
                        this.code = it.code.toString()
                        this.timestamp = System.currentTimeMillis()
                        this.data = PrinterEventData(
                            lastTransactionResponse.value!!.RRN,
                            "Printing"
                        )
                        this.status = it.message
                    }
                    MqttHelper.sendPayload(MqttTopics.PRINTING_RECEIPT, printerEvent)
                    //_message.value = Event("")
                    if (it.message.equals("sms", true).not())
                        _finish.value = Event(true)
                }
                throwable?.let {
                    when (it) {
                        is POSPrinterException -> {
                            _showPrinterError.value = Event(it.localizedMessage)
                            _message.value = Event("Error: ${it.localizedMessage}")
                            Timber.e(it)
                            printerEvent.apply {
                                this.event = MqttEvents.PRINTING_RECEIPT.event
                                this.code = "-1"
                                this.timestamp = System.currentTimeMillis()
                                this.data = PrinterEventData(
                                    lastTransactionResponse.value!!.RRN,
                                    it.localizedMessage
                                )
                                this.status = it.message
                            }
                            MqttHelper.sendPayload(MqttTopics.PRINTING_RECEIPT, printerEvent)
                        }
                        is UnknownHostException, is SocketException -> {
                            _message.value =
                                Event("Connection Error::${it.localizedMessage}")
                            Timber.e(it.localizedMessage)
                        }
                        is NibssClientException -> {
                            it.nibssError?.let { nibssError ->
                                Timber.e(nibssError.toString())
                                if (nibssError.possibleSolution!! == "Attempt configure terminal") {
                                    Prefs.remove(PREF_CONFIG_DATA)
                                    Prefs.remove(PREF_KEYHOLDER)
                                    _shouldRefreshNibssKeys.value = Event(true)
                                }
                                _message.value = Event(nibssError.error ?: "Error")
                            }
                        }
                        else -> {
                            _message.value = Event(it.localizedMessage ?: "Unknown exception")
                            Timber.e(it)
                        }
                    }
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
        _showPrintDialog.value = Event(
            lastTransactionResponse.value!!.buildSMSText(remark.value ?: "")
                .toString()
        )
    }

    private fun printReceipt(context: Context): Single<PrinterResponse> {
        val transactionResponse = lastTransactionResponse.value ?: gatewayErrorTransactionResponse(
            amountLong,
            TransactionType.PURCHASE,
            isoAccountType!!
        )
            .apply {
                this.cardExpiry = ""
                this.cardHolder = customerName.value ?: ""
            }
        return if (Build.MODEL.equals("Pro", true) || Build.MODEL.equals(
                "P3",
                true
            )
        ) transactionResponse.print(context, remark.value ?: "")
            .subscribeOn(Schedulers.io()) else {
            _showPrintDialog.postValue(
                Event(
                    transactionResponse.buildSMSText(remark.value ?: "").toString()
                )
            )
            Single.just(PrinterResponse(0, "SMS"))
        }
    }

    fun sendSmS(number: String) {
        val map = JsonObject().apply {
            addProperty("from", "NetPlus")
            addProperty("to", "+234${number.substring(1)}")
            addProperty(
                "message",
                lastTransactionResponse.value!!.buildSMSText(remark.value ?: "").toString()
            )
        }
        Timber.e("payload: $map")
        val auth = "Bearer ${Prefs.getString(PREF_APP_TOKEN, "")}"
        val body: RequestBody = map.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val smsEvent = MqttEvent<SMSEvent>()

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


    fun isVend(vend: Boolean) {
        isVend = vend
    }

    private fun sendVendResponse(context: Context, out: String) {
        Single.fromCallable {
            Socket().run {
                connect(InetSocketAddress("vend.netpluspay.com", 3535))
                val reader = BufferedReader(InputStreamReader(getInputStream()))
                Timber.e(reader.readLine())
                val printWriter = PrintWriter(getOutputStream(), true)
                printWriter.println(out)
                reader.readLine()
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {

            }
            .subscribe { t1, t2 ->
                t1?.let {
                    Timber.e(it)
                    //Toast.makeText(context, "received", Toast.LENGTH_SHORT).show()
                }
                t2?.let {
                    Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()
                    Timber.e(it)
                }
            }.disposeWith(compositeDisposable)
    }
}