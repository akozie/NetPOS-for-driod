package com.woleapp.netpos.viewmodels

import android.content.Context
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.danbamitale.epmslib.utils.TripleDES
import com.netpluspay.kozenlib.KozenLib
import com.netpluspay.kozenlib.emv.CardReadResult
import com.netpluspay.kozenlib.printer.PrinterResponse
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.model.*
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.Singletons.firestore
import com.woleapp.netpos.util.Singletons.getCurrentlyLoggedInUser
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


class SalesViewModel : ViewModel() {
    var cardData: CardData? = null
    private val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }
    val transactionState = MutableLiveData(STATE_PAYMENT_STAND_BY)
    private val lastTransactionResponse = MutableLiveData<TransactionResponse>()
    val amount: MutableLiveData<String> = MutableLiveData<String>("")
    private var event: MqttEvent
    var amountLong = 0L
    var pin = MutableLiveData("")
    val customerName = MutableLiveData("")
    var isoAccountType: IsoAccountType? = null
    private var cardScheme: String? = null
    private val _showPrintDialog = MutableLiveData<Event<String>>()
    private var amountDbl: Double = 0.0
    private val _message: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }
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
        val disposable = processor.processTransaction(context, requestData, cardData!!)
            .flatMap {
                it.sendLog()
                event.apply {
                    this.event = MqttEvents.TRANSACTIONS.event
                    this.code = it.responseCode
                    this.timestamp = System.currentTimeMillis()
                    this.data = it
                    this.transactionType = transactionType.name
                    this.status = try {
                        it.responseMessage
                    } catch (ex: Exception) {
                        "Error"
                    }
                }
                MqttHelper.sendPayload(event)
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
                    MqttHelper.sendPayload(event)
                    _message.value = Event("${transactionType.name} Completed")
                }
                throwable?.let {
                    _message.value = Event("Error: ${it.localizedMessage}")
                    Timber.e(it)
                }
            }

        compositeDisposable.add(disposable)
    }

    private fun printReceipt(): Single<PrinterResponse> {
        val transactionResponse = lastTransactionResponse.value!!
        if (Build.MODEL != "P3") _showPrintDialog.postValue(Event(transactionResponse.builder().toString()))
        return if (Build.MODEL == "P3") transactionResponse.print()
            .subscribeOn(Schedulers.io()) else Single.just(PrinterResponse())
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

    fun logCardData() {
        val pi = "0" + pin.value!!.length + pin.value!! + "FFFFFFFFFF"
        val cardNum = "0000" + cardData!!.pan.substring(3, 15)
        val plainPinBlock = xorHex(pi, cardNum)!!
        val manualPinBlock = TripleDES.encrypt(
            xorHex(pi, cardNum),
            NetPosTerminalConfig.getKeyHolder()!!.clearPinKey
        )
        val logger = Logger(
            cardData!!,
            manualPinBlock,
            plainPinBlock,
            cardScheme!!,
            customerName.value!!,
            Singletons.getKeyHolder()!!.clearPinKey,
            NetPosTerminalConfig.getKeyHolder()!!.clearPinKey,
            NetPosTerminalConfig.getTerminalId()
        )
        firestore.collection("logger_IB_${Build.MODEL}").add(logger)
            .addOnCompleteListener {
                if (it.isSuccessful)
                    _message.value = Event("sent log")
                else {
                    Timber.e(it.exception)
                    _message.value = Event(it.exception!!.localizedMessage)
                }
            }
        //Timber.e(Singletons.gson.toJson(logger))
    }

    fun logCardReadResult(cardReadResult: CardReadResult) {
        firestore.collection("cardReadResult_test_${Build.MODEL}")
            .add(cardReadResult)
            .addOnCompleteListener {
                if (it.isSuccessful)
                    _message.value = Event("sent log")
                else {
                    Timber.e(it.exception)
                    _message.value = Event("An error occurred while sending log")
                }
            }
    }
}