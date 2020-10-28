package com.woleapp.netpos.viewmodels

import android.content.Context
import androidx.lifecycle.*
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.netplus.sunyardlib.ReceiptBuilder
import com.socsi.smartposapi.printer.PrintRespCode
import com.woleapp.netpos.BuildConfig
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
    private val _message: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    init {
        val user = getCurrentlyLoggedInUser()
        event = MqttEvent(
            user!!.netplus_id!!,
            user.business_name!!,
            NetPosTerminalConfig.getTerminalId(),
            "JKEWUBUBIBSBBWUBUWBYUB89243"
        )
    }

    val message: LiveData<Event<String>>
        get() = _message

    fun setCustomerName(name: String) {
        customerName.value = name
    }

    fun makePayment(context: Context, transactionType: TransactionType = TransactionType.PURCHASE) {
        val amountDbl = (amount.value!!.toDoubleOrNull() ?: kotlin.run {
            _message.value = Event("Enter a valid amount")
            return
        }) * 100
        Timber.e(cardData.toString())
        val hexStringPin = "042539FFFFFFFFFF"
        val hexCardNum = "0000009181414442"

        Timber.e("Pin to make transaction: ${xorHex(hexStringPin, hexCardNum)}")
        val configData = NetPosTerminalConfig.getConfigData() ?: kotlin.run {
            _message.value =
                Event("Terminal has not been configured, restart the application to configure")
            return
        }
        val keyHolder = NetPosTerminalConfig.getKeyHolder()!!
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
                lastTransactionResponse.postValue(it)

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
                        this.code = it.name
                        this.timestamp = System.currentTimeMillis()
                        this.data = PrinterEventData(lastTransactionResponse.value!!.RRN, it.name)
                        this.status = it.name
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

    private fun printReceipt(): Single<PrintRespCode> {
        val transactionResponse = lastTransactionResponse.value!!
        return ReceiptBuilder()
            .apply {
                appendAID(transactionResponse.AID)
                appendAddress("NETPOS")
                appendAmount(
                    transactionResponse.amount.div(100).formatCurrencyAmount("\u20A6")
                )
                appendAppName("NetPOS")
                appendAppVersion(BuildConfig.VERSION_NAME)
                appendAuthorizationCode(transactionResponse.authCode)
                appendCardHolderName(transactionResponse.cardHolder)
                appendCardNumber(transactionResponse.maskedPan)
                appendCardScheme("Card ${transactionResponse.cardLabel}")
                appendDateTime(transactionResponse.transactionTimeInMillis.formatDate())
                appendRRN(transactionResponse.RRN)
                appendStan(transactionResponse.STAN)
                appendTerminalId(NetPosTerminalConfig.getTerminalId())
                appendTransactionType(transactionResponse.transactionType.name)
                appendTransactionStatus(if (transactionResponse.responseCode == "00") "Approved" else "Declined")
                appendResponseCode(
                    "${transactionResponse.responseCode}\nMessage: ${
                        try {
                            transactionResponse.responseMessage
                        } catch (ex: Exception) {
                            "Error"
                        }
                    }"
                )
            }.isCustomerCopy().print().subscribeOn(Schedulers.io())
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

}