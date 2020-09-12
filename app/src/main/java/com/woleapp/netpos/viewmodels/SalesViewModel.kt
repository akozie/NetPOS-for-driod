package com.woleapp.netpos.viewmodels

import android.content.Context
import androidx.lifecycle.*
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.netplus.sunyardlib.ReceiptBuilder
import com.socsi.smartposapi.printer.PrintRespCode
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.*
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
    var amountLong = 0L
    var pin: String? = null
    val customerName = MutableLiveData("")
    private val _message: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }
    val message: LiveData<Event<String>>
        get() = _message

    fun setCardPin(pinFromPad:String){
        Timber.e("setting pin: $pinFromPad")
        pin = pinFromPad
        Timber.e("Pin Set: $pin")
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
            _message.value = Event("Please wait a bit, terminal configuration in progress")
            return
        }
        val keyHolder = NetPosTerminalConfig.getKeyHolder()!!
        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.getConnectionData(),
            keyHolder,
            configData
        )
        this.amountLong = amountDbl.toLong()
        val requestData = TransactionRequestData(transactionType, amountLong, 0L)
        val processor = TransactionProcessor(hostConfig)
        transactionState.value = STATE_PAYMENT_STARTED
        val disposable = processor.processTransaction(context, requestData, cardData!!)
            .flatMap {
                Timber.e(it.toString())
                Timber.e(it.responseMessage)
                Timber.e("${it.isApproved}")
                it.cardHolder = customerName.value!!
                lastTransactionResponse.postValue(it)
//                if (!it.isApproved) {
//                    _message.postValue(Event("Transaction not approved"))
//                    throw Exception("Transaction not approved")
//                }
                //transactionState.postValue(STATE_PAYMENT_APPROVED)
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
                appendCardScheme("card scheme")
                appendDateTime(transactionResponse.transactionTimeInMillis.formatDate())
                appendRRN(transactionResponse.RRN)
                appendStan(transactionResponse.STAN)
                appendTerminalId(NetPosTerminalConfig.getTerminalId())
                appendTransactionType(transactionResponse.transactionType.name)
                appendTransactionStatus(if (transactionResponse.responseCode == "00") "Approved" else "Declined")
                appendResponseCode("${transactionResponse.responseCode}")
            }.print().subscribeOn(Schedulers.io())
    }


    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

}