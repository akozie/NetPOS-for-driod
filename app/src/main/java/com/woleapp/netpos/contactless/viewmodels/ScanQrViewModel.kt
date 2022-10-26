package com.woleapp.netpos.contactless.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.model.*
import com.woleapp.netpos.contactless.network.Repository
import com.woleapp.netpos.contactless.util.AppConstants.QR_TRANSACTION_POST_REQUEST_PAYLOAD
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.stringToBase64
import com.woleapp.netpos.contactless.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

@HiltViewModel
class ScanQrViewModel @Inject constructor(
    private val repository: Repository,
    private val gson: Gson
) : ViewModel() {
    private val compositeDisposable = CompositeDisposable()
    private val _sendQrToServerResponse: MutableLiveData<Resource<PostQrToServerResponse>> =
        MutableLiveData()
    val sendQrToServerResponse: LiveData<Resource<PostQrToServerResponse>> get() = _sendQrToServerResponse
    private val _isVerveCard: MutableLiveData<Boolean> = MutableLiveData(false)

    private val _sendQrToServerResponseVerve: MutableLiveData<Resource<PostQrToServerVerveResponseModel>> =
        MutableLiveData()
    val sendQrToServerResponseVerve: LiveData<Resource<PostQrToServerVerveResponseModel>> get() = _sendQrToServerResponseVerve

    private val _transactionResponseFromVerve: MutableLiveData<Resource<Any?>> =
        MutableLiveData()
    val transactionResponseFromVerve: LiveData<Resource<Any?>> get() = _transactionResponseFromVerve

    private val _cardScheme: MutableLiveData<String> = MutableLiveData("")

    fun postScannedQrRequestToServer(qrData: PostQrToServerModel) {
        if (_isVerveCard.value == true) _sendQrToServerResponseVerve.value =
            Resource.loading(null) else _sendQrToServerResponse.value = Resource.loading(null)
        compositeDisposable.add(
            repository.postScannedQrRequestToServer(qrData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap {
                    Single.just(it.body())
                }
                .subscribe { postQrResponse, error ->
                    postQrResponse?.let {
                        val serverResponse: Any? = if (it.has("TermUrl")) {
                            gson.fromJson(
                                it,
                                PostQrToServerResponse::class.java
                            )
                        } else if (it.get("status").asString.lowercase() != "failed") {
                            gson.fromJson(it, PostQrToServerVerveResponseModel::class.java)
                        } else {
                            null
                        }
                        if (serverResponse is PostQrToServerResponse) {
                            _sendQrToServerResponse.value = Resource.success(serverResponse)
                        } else if (serverResponse is PostQrToServerVerveResponseModel) {
                            _sendQrToServerResponseVerve.value =
                                Resource.success(serverResponse)
                        } else {
                            if (_isVerveCard.value == true) _sendQrToServerResponseVerve.value =
                                Resource.error(null) else _sendQrToServerResponse.value =
                                Resource.error(null)
                        }
                    }
                    error?.let {
                        if (_isVerveCard.value == true) {
                            _sendQrToServerResponseVerve.value =
                                if (it is SocketTimeoutException) Resource.timeOut(null) else Resource.error(
                                    null
                                )
                        } else {
                            _sendQrToServerResponse.value =
                                if (it is SocketTimeoutException) Resource.timeOut(null) else Resource.error(
                                    null
                                )
                        }
                        Timber.d(gson.toJson(it))
                    }
                }
        )
    }

    fun sendOtpForVerveCard(otp: String) {
        _transactionResponseFromVerve.value = Resource.loading(null)
        sendQrToServerResponseVerve.value?.data?.let {
            val otpDetails =
                stringToBase64(it.transId + ":" + it.result + ":" + otp.trim() + ":" + it.provider)
            val otpPayLoad = SendOtpForVerveCardModel(otpDetails.removeSuffix("\\n"))
            compositeDisposable.add(
                repository.consummateTransactionBySendingOtp(otpPayLoad)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap { response ->
                        Single.just(response.body())
                    }
                    .subscribe { data, error ->
                        data?.let { transResp ->
                            val transResponse = if (transResp.has("narration")) {
                                gson.fromJson(transResp, QrTransactionResponseModel::class.java)
                            } else {
                                gson.fromJson(transResp, VerveTransactionResponse::class.java)
                            }
                            _transactionResponseFromVerve.value =
                                Resource.success(transResponse)
                        }
                        error?.let { throwable ->
                            Timber.d("ERROR_FROM_VP%s", throwable.localizedMessage)
                            _transactionResponseFromVerve.value =
                                if (throwable is SocketTimeoutException) Resource.timeOut(null) else Resource.error(
                                    null
                                )
                        }
                    }
            )
        }
    }

    fun saveTheQrToSharedPrefs(qrData: PostQrToServerModel) {
        Prefs.putString(QR_TRANSACTION_POST_REQUEST_PAYLOAD, gson.toJson(qrData))
    }

    fun deleteQrPostPayloadFromSharedPrefs() {
        Prefs.remove(QR_TRANSACTION_POST_REQUEST_PAYLOAD)
    }

    fun setScannedQrIsVerveCard(boolean: Boolean) {
        _isVerveCard.value = boolean
    }

    fun retrieveQrPostPayloadFromSharedPrefs(): PostQrToServerModel? {
        val payload = Prefs.getString(QR_TRANSACTION_POST_REQUEST_PAYLOAD, "")
        return if (!payload.isNullOrEmpty()) {
            gson.fromJson(payload, PostQrToServerModel::class.java)
        } else null
    }

    fun setCardScheme(cardScheme: String) {
        _cardScheme.value = cardScheme
    }

    fun getCardScheme(): String = _cardScheme.value ?: ""

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}
