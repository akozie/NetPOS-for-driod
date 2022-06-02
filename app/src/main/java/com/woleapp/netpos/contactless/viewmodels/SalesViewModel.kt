package com.woleapp.netpos.contactless.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.HostConfig
import com.danbamitale.epmslib.entities.TransactionRequestData
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.responseMessage
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.danbamitale.epmslib.utils.MessageReasonCode
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.database.AppDatabase
import com.woleapp.netpos.contactless.nibss.NetPosTerminalConfig
import com.woleapp.netpos.contactless.util.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket



class SalesViewModel : ViewModel() {
    private var isVend: Boolean = false
    var cardData: CardData? = null
    private val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }
    val transactionState = MutableLiveData(STATE_PAYMENT_STAND_BY)
    private val lastTransactionResponse = MutableLiveData<TransactionResponse>()
    val amount: MutableLiveData<String> = MutableLiveData<String>("")
    val cashback: MutableLiveData<String> = MutableLiveData<String>("")
    var amountLong = 0L
    var cashbackLong = 0L
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

    private val _showReceiptTypeMutableLiveData = MutableLiveData<Event<Boolean>>()

    val showReceiptType: LiveData<Event<Boolean>>
        get() = _showReceiptTypeMutableLiveData


    fun setCustomerName(name: String) {
        customerName.value = name
    }

    fun validateField() {
        amountDbl = (amount.value!!.toDoubleOrNull() ?: kotlin.run {
            _message.value = Event("Enter a valid amount")
            return
        }) * 100
        this.amountLong = amountDbl.toLong()
        this.cashbackLong = cashback.value?.toDoubleOrNull()?.times(100)?.toLong() ?: 0L
        _getCardData.value = Event(true)
    }

    fun makePayment(context: Context, transactionType: com.danbamitale.epmslib.entities.TransactionType = com.danbamitale.epmslib.entities.TransactionType.PURCHASE) {
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
            NetPosTerminalConfig.connectionData,
            keyHolder,
            configData
        )
        //IsoAccountType.
        this.amountLong = amountDbl.toLong()
        val requestData =
            TransactionRequestData(transactionType, amountLong, cashbackLong, accountType = isoAccountType!!)
        val processor = TransactionProcessor(hostConfig)
        transactionState.value = STATE_PAYMENT_STARTED
        processor.processTransaction(context, requestData, cardData!!)
            .onErrorResumeNext {
                processor.rollback(context, MessageReasonCode.Timeout)
            }
            .flatMap {
                it.amount = amountLong
                if (it.responseCode == "A3") {
                    Prefs.remove(PREF_CONFIG_DATA)
                    Prefs.remove(PREF_KEYHOLDER)
                    _shouldRefreshNibssKeys.postValue(Event(true))
                }
                it.cardHolder = customerName.value!!
                it.cardLabel = cardScheme!!
                lastTransactionResponse.postValue(it)
                Timber.e(it.toString())
                Timber.e(it.responseCode)
                Timber.e(it.responseMessage)
                _message.postValue(Event(if (it.responseCode == "00") "Transaction Approved" else "Transaction Not approved"))
                printReceipt(it)
                AppDatabase.getDatabaseInstance(context).transactionResponseDao().insertNewTransaction(it)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                transactionState.value = STATE_PAYMENT_STAND_BY
            }.subscribe { t1, throwable ->
                t1?.let {

                }
                throwable?.let {
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
        _showPrintDialog.value = Event(
            lastTransactionResponse.value!!.buildSMSText(remark.value ?: "")
                .toString()
        )
    }

    private fun printReceipt(transactionResponse: TransactionResponse) {
         transactionResponse.apply {
                this.cardExpiry = ""
                this.cardHolder = customerName.value ?: ""
            }
        Timber.e(transactionResponse.toString())
        _showPrintDialog.postValue(
            Event(transactionResponse.buildSMSText(remark.value ?: "").toString())
        )
    }

    fun finish() {
        _finish.value = Event(true)
    }


    fun isVend(vend: Boolean) {
        isVend = vend
    }

    private fun sendVendResponse(context: Context, out: String) {
        Single.fromCallable {
            Socket().run {
                soTimeout = 120_000
                connect(InetSocketAddress(VEND_IP, VEND_PORT))
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