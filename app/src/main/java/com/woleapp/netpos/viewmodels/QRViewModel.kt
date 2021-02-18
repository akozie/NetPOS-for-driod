package com.woleapp.netpos.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.util.Event
import com.woleapp.netpos.util.Singletons
import com.woleapp.netpos.util.disposeWith
import com.woleapp.netpos.util.encodeAsBitmap
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import timber.log.Timber
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class QRViewModel : ViewModel() {
    private var retryAttempts = 1
    var stillHasRetryAttempts = true
    private val masterPassQRService = StormApiClient.getMasterPassQrServiceInstance()
    private val nibssQRService = StormApiClient.getNibssQRServiceInstance()
    private val disposable = CompositeDisposable()
    private val _masterPassQrBitmap = MutableLiveData<Event<Bitmap>>()
    val masterPassQrBitmap: LiveData<Event<Bitmap>>
        get() = _masterPassQrBitmap

    private val _message = MutableLiveData<Event<String>>()
    val message: LiveData<Event<String>>
        get() = _message

    private val _qrErrorMessage = MutableLiveData<Event<String>>()
    val qrErrorMessage: LiveData<Event<String>>
        get() = _qrErrorMessage

    private val lastNibssOrderNumber = MutableLiveData<String>()

    private val _nibssQRBitmap = MutableLiveData<Event<Bitmap>>()
    val nibssQRBitmap: LiveData<Event<Bitmap>>
        get() = _nibssQRBitmap

    private val _reQuerying = MutableLiveData<Event<Boolean>>()

    val reQuerying: LiveData<Event<Boolean>>
        get() = _reQuerying


    fun getMasterPassQr(amount: Double) {
        Timber.e("Get masterpass")
        val qrRequestBody = JsonObject()
        val user = Singletons.getCurrentlyLoggedInUser()!!
        qrRequestBody.apply {
            addProperty("amount", amount.toString())
            addProperty("order_id", UUID.randomUUID().toString())
            addProperty("merchant_id", user.netplus_id)
            addProperty("currency_code", "NGN")
            addProperty("country_code", "NG")
            addProperty("business_name", user.business_name)
            addProperty("merchant_city", "Lagos")
        }
        masterPassQRService.getStaticQr(qrRequestBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    val bmp = BitmapFactory.decodeStream(it.byteStream())
                    bmp?.let { bitmap ->
                        Timber.e("gotten and amount is $amount")
                        _masterPassQrBitmap.value = Event(bitmap)
                    }
                }
                t2?.let { error ->
                    val httpException = error as? HttpException
                    httpException?.let {
                        //Timber.e(it)
                        Timber.e("body ${it.response()?.errorBody()?.string()}")
                        Timber.e("message ${it.message()}")
                        Timber.e(it.message ?: "Error")
                    }
                    _qrErrorMessage.value = Event("Error: ${error.localizedMessage ?: "Error"}")
                    _message.value = Event(
                        "An error occurred while fetching QR"
                    )
                    //Timber.e(it)
                }
            }.disposeWith(disposable)
    }

    fun getNibssQR(amount: Double) {
        val start: Long = 111_111_111_111
        val end: Long = 999_999_999_999
        val range1 = (start..end).random()
        val range2 = (start..end).random()
        lastNibssOrderNumber.value =
            "${
                SimpleDateFormat(
                    "yMM",
                    Locale.getDefault()
                ).format(Date(System.currentTimeMillis()))
            }$range1$range2"
        val jsonObject = JsonObject()
        jsonObject.addProperty("amount", amount.toString())
        jsonObject.addProperty("order_no", lastNibssOrderNumber.value!!)
        nibssQRService.getQr(jsonObject)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                if (it.returnCode.isNullOrEmpty() || it.returnCode != "Success" || it.codeUrl.isNullOrEmpty())
                    throw Exception("Could not fetch QR code")
                Single.just(encodeAsBitmap(it.codeUrl, 150, 150))
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    _nibssQRBitmap.value = Event(it)
                    runHandler()
                }
                t2?.let {
                    _message.value = Event("Error ${it.localizedMessage}")
                }
            }
            .disposeWith(disposable)
    }

    private fun runHandler() {
        if (retryAttempts > 15) {
            stillHasRetryAttempts = false
            _message.value = Event("Too many attempts without response")
            return
        }
        Handler().postDelayed({
            queryTransaction()
        }, 4000)
    }

    private fun queryTransaction() {
        retryAttempts += 1
        _reQuerying.value = Event(true)
        lastNibssOrderNumber.value?.let {
            val jsonObject = JsonObject()
            jsonObject.addProperty("order_no", it)
            nibssQRService.queryTransactionStatus(jsonObject)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _reQuerying.value = Event(false) }
                .subscribe { t1, t2 ->
                    t1?.let { nibssQRResponse ->
                        when (nibssQRResponse.returnCode) {
                            "Paying" -> runHandler()
                            "Success" -> {
                                stillHasRetryAttempts = false
                                _message.value = Event("Success, payment confirmed")
                            }
                            else -> {
                                _message.value = Event("Failed, payment failed")
                                stillHasRetryAttempts = false
                            }
                        }
                    }
                    t2?.let {
                        retryAttempts -= 1
                        _message.value = Event("Retrying")
                    }
                }.disposeWith(disposable)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }
}