package com.woleapp.netpos.viewmodels

import android.content.Context
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.netpluspay.nibssclient.models.*
import com.netpluspay.nibssclient.service.NibssApiWrapper
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.model.*
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.network.StormUtilitiesApiService
import com.woleapp.netpos.util.*
import io.reactivex.Single

import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import timber.log.Timber


class UtilitiesViewModel : ViewModel() {
    var cardData: CardData? = null
    private val customerName = MutableLiveData("")
    private var amountLong = 0L
    private var cardScheme: String? = null
    private var isoAccountType: IsoAccountType? = null
    private lateinit var remark: String
    private val compositeDisposable = CompositeDisposable()
    private val lastTransactionResponse = MutableLiveData<TransactionResponse>()
    private lateinit var stormUtilitiesApiService: StormUtilitiesApiService
    private val _initiateBillsPayment = MutableLiveData<Event<Long>>()
    val initiateBillsPayment: LiveData<Event<Long>>
        get() = _initiateBillsPayment
    private val gson = Gson()
    val payloadMutableLiveData: MutableLiveData<UtilitiesPayload> by lazy {
        MutableLiveData<UtilitiesPayload>().also {
            it.value = UtilitiesPayload()
        }
    }
    private val _showProgressMutableLiveData: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>().also {
            it.value = Event(false)
        }
    }

    private val _shouldRefreshNibssKeys = MutableLiveData<Event<Boolean>>()
    val shouldRefreshNibssKeys: LiveData<Event<Boolean>>
        get() = _shouldRefreshNibssKeys

    private val _validateResponse = MutableLiveData<Event<ValidateBillResponse>>()

    val validateBillResponse: LiveData<Event<ValidateBillResponse>>
        get() = _validateResponse

    val showProgress: LiveData<Event<Boolean>> = _showProgressMutableLiveData
    private val _showPrintDialog = MutableLiveData<Event<String>>()
    val showPrintDialog: LiveData<Event<String>>
        get() = _showPrintDialog
    private val _showPrinterError = MutableLiveData<Event<String>>()

    val showPrinterError: LiveData<Event<String>>
        get() = _showPrinterError

    private val _smsSent = MutableLiveData<Event<Boolean>>()
    val smsSent: LiveData<Event<Boolean>>
        get() = _smsSent

    fun setStormUtilitiesApiService(stormUtilitiesApiService: StormUtilitiesApiService) {
        this.stormUtilitiesApiService = stormUtilitiesApiService
    }

    private val _message = MutableLiveData<Event<String>>()

    val message: LiveData<Event<String>>
        get() = _message

    private val _result = MutableLiveData<Event<NetworkResponse>>()
    val result: LiveData<Event<NetworkResponse>> = _result

    private val _billResponse = MutableLiveData<ValidateBillResponse>()

    private var errorResponse: ErrorNetworkResponse? = null
    /*fun setUtilityPrice(utilityAmount: Long) {
        val utilitiesPayload = payloadMutableLiveData.value
        payloadMutableLiveData.value = utilitiesPayload?.apply {
            amount = utilityAmount
        }
    }*/

    fun setUtilityService(utilityService: String) {
        val utilitiesPayload = payloadMutableLiveData.value
        payloadMutableLiveData.value = utilitiesPayload?.apply {
            service = utilityService.replace(" ", "")
        }
    }

    fun setUtilityBillType(utilityBillType: String) {
        val utilitiesPayload = payloadMutableLiveData.value
        payloadMutableLiveData.value = utilitiesPayload?.apply {
            billType = utilityBillType
        }
    }

    fun setUtilityProvider(utilityProvider: String) {
        val utilitiesPayload = payloadMutableLiveData.value
        //_message.value = Event(utilityProvider)
        payloadMutableLiveData.value = utilitiesPayload?.apply {
            provider = utilityProvider.replace(" ", "")
        }
    }

    fun setUtilityPackage(utilityPackage: String) {
        val utilitiesPayload = payloadMutableLiveData.value
        payloadMutableLiveData.value = utilitiesPayload?.apply {
            billPackage = utilityPackage
        }
    }

    fun setUtilityServiceType(utilityServiceType: String) {
        val utilitiesPayload = payloadMutableLiveData.value
        payloadMutableLiveData.value = utilitiesPayload?.apply {
            serviceType = utilityServiceType
        }
    }

    fun validateUtilityBill() {
        val utilitiesPayload = payloadMutableLiveData.value
        if (utilitiesPayload?.destinationAccount.isNullOrEmpty()) {
            _message.value = Event("Please enter the destination number")
            return
        }
        if (utilitiesPayload?.stringAmount.isNullOrEmpty())
            utilitiesPayload?.stringAmount = "0"
        utilitiesPayload?.amount = utilitiesPayload?.stringAmount?.replace(",", "")?.toFloat() ?: 0f
        if (utilitiesPayload?.billType == "POWER" && utilitiesPayload.amount < 1000) {
            _message.value = Event("Amount should not be less than \u20A61000")
            return
        }
        if (utilitiesPayload?.amount!! < 25) {
            _message.value = Event("Amount should not be less than \u20A625")
            return
        }
        Timber.e("Principal Amount: ${utilitiesPayload.amount}")
        val next = when (utilitiesPayload.billType) {
            "POWER", "TV" -> ::verifyDestinationAccount
            else -> ::checkHasSufficientBalance
        }
        payloadMutableLiveData.value = utilitiesPayload
        next.invoke()
    }

    private fun verifyDestinationAccount() {
        val utilitiesPayload = payloadMutableLiveData.value
        stormUtilitiesApiService.validateBill(utilitiesPayload!!)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { _showProgressMutableLiveData.value = Event(true) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ValidateBillResponse> {
                override fun onSuccess(t: ValidateBillResponse) {
                    Timber.e(t.toString())
                    if (t.billVerifiedOnline == "FALSE" || t.billVerifiedOnline == null) {
                        val errorResponse =
                            ErrorNetworkResponse("Could not verify destination Number: ${utilitiesPayload.destinationAccount}")
                        _result.value = Event(errorResponse)
                        return
                    }
                    _billResponse.value = t
                    checkHasSufficientBalance()
                }

                override fun onSubscribe(d: Disposable) {
                    d.disposeWith(compositeDisposable)
                }

                override fun onError(e: Throwable) {
                    val errorResponse =
                        ErrorNetworkResponse("Could not verify destination Number: ${utilitiesPayload.destinationAccount}")
                    _result.value = Event(errorResponse)
                }

            })
    }

    fun checkHasSufficientBalance() {
        val validateBillResponse = _billResponse.value
        val utilitiesPayload = payloadMutableLiveData.value!!
        val billResponse = validateBillResponse ?: ValidateBillResponse(
            billAccountId = utilitiesPayload.destinationAccount
        )
        billResponse.apply {
            provider = utilitiesPayload.provider
            billPackage = utilitiesPayload.billPackage
            fees = "\u20A6${0}"
            amount = "\u20A6${utilitiesPayload.amount}"
        }
        _validateResponse.value = Event(billResponse)
    }


    /*fun getServiceFee() {
        val validateBillResponse = _billResponse.value
        val utilitiesPayload = payloadMutableLiveData.value!!
        stormUtilitiesApiService.getServiceFee(utilitiesPayload.provider)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _showProgressMutableLiveData.value = Event(true) }
            .subscribe(object : SingleObserver<ServiceFeeResponse> {
                override fun onSuccess(t: ServiceFeeResponse) {
                    Timber.e( t.toString())
                    val billResponse = validateBillResponse ?: ValidateBillResponse(
                        billAccountId = utilitiesPayload.destinationAccount
                    )
                    billResponse.apply {
                        provider = utilitiesPayload.provider
                        billPackage = utilitiesPayload.billPackage
                        fees = "\u20A6${0}"
                        amount = "\u20A6${utilitiesPayload.amount}"
                    }
                    _validateResponse.value = Event(billResponse)
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {
                    val error = e.getResponseBody()
                    val errorResponse =
                        gson.fromJson<ErrorResponse>(error, ErrorResponse::class.java)
                    _result.value = Event(errorResponse)
                }

            })
    }*/

    fun initiatePayment() {
        val utilitiesPayload = payloadMutableLiveData.value!!
        utilitiesPayload.apply {
            remark = "$billType\\$billPackage\\$provider\\$destinationAccount"
        }
        amountLong = utilitiesPayload.amount.toLong().times(100)
        _initiateBillsPayment.value = Event(utilitiesPayload.amount.toLong().times(100))
        //payBill()
    }

    private fun payBill(context: Context) {
        _showProgressMutableLiveData.postValue(Event(true))
        val utilitiesPayload = payloadMutableLiveData.value
        _message.postValue(Event("Processing ${utilitiesPayload?.billType} request"))
        stormUtilitiesApiService.payBills(utilitiesPayload!!)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { _showProgressMutableLiveData.postValue(Event(true)) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    val data = it.data
                    Timber.e(data?.reference!!)
                    remark.plus("\n${utilitiesPayload.billType} Payment Success")
                    if (utilitiesPayload.billType == "POWER") {
                        it.message = if (data.token.isNullOrEmpty())
                            "Your power payment was successful but no token was generated, please contact support"
                        else {
                            "${it.message}\n\nMeter Token: ${data.token}"
                            remark.plus("\nMeter Token: ${data.token}")
                        }
                    }
                    _result.value = Event(it)
                    _showProgressMutableLiveData.value = Event(false)
                    printReceipt(context)
                }
                t2?.let {
                    errorResponse =
                        if (it.isHttpException()) {
                            _message.value =
                                Event("${utilitiesPayload.billType} request failed, reversing transaction 😂")
                            val error = it.getResponseBody()
                            gson.fromJson(error, ErrorNetworkResponse::class.java)
                        } else
                            ErrorNetworkResponse(it.localizedMessage ?: "")

                    remark.plus("\n${utilitiesPayload.billType} Payment Failed")
                    Timber.e(it.toString())
                    reverseTransaction(context)
                }
            }.disposeWith(compositeDisposable)
    }

    private fun reverseTransaction(context: Context) {
        val requestData = RefundTransactionParams(
            cardData!!,
            lastTransactionResponse.value!!,
            messageReasonCode = MessageReasonCode.CompletedPartially,
            accountType = isoAccountType!!
        ).apply {
            fundWallet = false
        }
        NibssApiWrapper.refundTransaction(
            context,
            requestData
        ).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                _showProgressMutableLiveData.value = Event(false)
                _result.value = Event(
                    errorResponse
                        ?: ErrorNetworkResponse("An unresolvable error occurred, contact administrator")
                )
                printReceipt(context)
            }
            .subscribe { t1, t2 ->
                t1?.let { response ->
                    Timber.e(response.toString())
                    lastTransactionResponse.value = response
                    if (response.responseCode == "06") {
                        errorResponse?.message =
                            "Could not process ${payloadMutableLiveData.value?.billType} payment, Transaction Reversed"
                    } else {
                        errorResponse?.message =
                            "Could not process ${payloadMutableLiveData.value?.billType} payment, Transaction could not be auto reversed, contact administrator"
                    }
                }
                t2?.let {
                    errorResponse?.message =
                        "Could not process ${payloadMutableLiveData.value?.billType} payment, Transaction could not be auto reversed, contact administrator"
                }
            }.disposeWith(compositeDisposable)

    }

    fun makePayment(context: Context, transactionType: TransactionType = TransactionType.PURCHASE) {
        _showProgressMutableLiveData.value = Event(true)
        val makePaymentParams =
            MakePaymentParams(amountLong, 0L, cardData, transactionType, isoAccountType!!).apply {
                fundWallet = false
            }
        NibssApiWrapper.makePayment(context, makePaymentParams)
            .flatMap {
                if (it.responseCode == "A3") {
                    Prefs.remove(PREF_CONFIG_DATA)
                    Prefs.remove(PREF_KEYHOLDER)
                    _shouldRefreshNibssKeys.postValue(Event(true))
                }
                it.cardHolder = customerName.value!!
                it.cardLabel = cardScheme!!
                lastTransactionResponse.postValue(it)
                //_message.postValue(Event(if (it.responseCode == "00") "Transaction Approved" else "Transaction Not approved"))
                if (it.responseCode == "00") {
                    payBill(context)
                } else {
                    _showProgressMutableLiveData.postValue(Event(false))
                    _result.postValue(Event(ErrorNetworkResponse("Transaction Declined")))
                    printReceipt(context)
                }
                AppDatabase.getDatabaseInstance(context).transactionResponseDao()
                    .insertNewTransaction(it)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, throwable ->
                t1?.let {

                }
                throwable?.let {
                    _message.value = Event(it.localizedMessage ?: "")
                    _showProgressMutableLiveData.value = Event(false)
                    _result.value = Event(ErrorNetworkResponse("Could Not Make Payment"))
                }
            }.disposeWith(compositeDisposable)
    }

    fun showReceiptDialog() {
        _showPrintDialog.value = Event(
            lastTransactionResponse.value!!.buildSMSText(remark)
                .toString()
        )
    }

    private fun printReceipt(context: Context) {
        _message.postValue(Event("Printing Receipt"))
        Timber.e(remark)
        val transactionResponse = lastTransactionResponse.value!!
        _showPrintDialog.postValue(
            Event(
                transactionResponse.buildSMSText(remark).toString()
            )
        )
    }

    fun setCustomerName(name: String) {
        customerName.value = name
    }

    fun setAccountType(accountType: IsoAccountType) {
        this.isoAccountType = accountType
    }

    fun setCardScheme(cardScheme: String?) {
        this.cardScheme = if (cardScheme.equals("no match", true)) "VERVE" else cardScheme
    }

    fun sendSmS(number: String) {
        val map = JsonObject().apply {
            addProperty("from", "NetPlus")
            addProperty("to", "+234${number.substring(1)}")
            addProperty("message", lastTransactionResponse.value!!.buildSMSText(remark).toString())
        }
        Timber.e("payload: $map")
        val auth = "Bearer ${Prefs.getString(PREF_APP_TOKEN, "")}"
        val body: RequestBody = map.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        StormApiClient.getSmsServiceInstance().sendSms(auth, body)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    _smsSent.value = Event(true)
                    Timber.e("Data $it")
                }
                t2?.let {
                    val httpException = it as? HttpException
                    httpException?.let { _ ->

                    }
                    _smsSent.value = Event(false)
                }
            }.disposeWith(compositeDisposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}
