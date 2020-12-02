package com.woleapp.netpos.viewmodels

import android.content.Context
import androidx.lifecycle.*
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.netpluspay.kozenlib.KozenLib
import com.netpluspay.kozenlib.printer.PrinterResponse
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.model.*
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.*
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
    var pin: String? = null
    val customerName = MutableLiveData("")
    var isoAccountType: IsoAccountType? = null
    private var cardScheme: String? = null
    private var amountDbl: Double = 0.0
    private val _message: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }
    private val _getCardData = MutableLiveData<Event<Boolean>>()

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

        //Timber.e("Pin to make transaction: ${xorHex(hexStringPin, hexCardNum)}")
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
        return transactionResponse.print().subscribeOn(Schedulers.io())
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

}