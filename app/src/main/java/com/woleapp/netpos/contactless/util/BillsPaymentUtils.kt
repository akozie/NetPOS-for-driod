package com.woleapp.netpos.contactless.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.network.StormApiClient
import com.woleapp.netpos.contactless.network.StormApiService
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import timber.log.Timber

fun checkBillsPaymentToken(): Boolean {
    val billsToken = Prefs.getString(PREF_BILLS_TOKEN, null)
    return !(billsToken.isNullOrEmpty() || JWTHelper.isExpired(billsToken))
}

fun getBillsToken(stormApiService: StormApiService): LiveData<Event<Boolean>> {
    val liveData = MutableLiveData<Event<Boolean>>()
    val credentials = JsonObject()
    credentials.addProperty("appname", "bills_payment")
    credentials.addProperty("password", "!L@ns33mfls")
    Timber.e("get resp")
    stormApiService.appToken(credentials)
        .flatMap {
            Timber.e(it.toString())
            if (it.success) {
                val billsCredentials = JsonObject()
                    .apply {
                        addProperty("username", "bills@netplusadvisory.com")
                        addProperty("password", "C0r3M3ltD0wn!!")
                    }
                stormApiService.userToken("Bearer ${it.token}", billsCredentials)
            } else {
                throw Exception("Login Failed")
            }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { res, error ->
            res?.let {
                Timber.e(it.toString())
                if (!it.success) {
                    liveData.value = Event(false)
                    return@let
                } else {
                    Prefs.putString(PREF_BILLS_TOKEN, it.token)
                    liveData.value = Event(true)
                }
            }
            error?.let {
                liveData.value = Event(false)
                Timber.e(it.localizedMessage)
            }
        }.disposeWith(CompositeDisposable())
    return liveData
}

fun checkAppToken(): Boolean {
    val appToken = Prefs.getString(PREF_USER_TOKEN, null)
    return !(appToken.isNullOrEmpty() || JWTHelper.isExpired(appToken))
}

fun getAppToken(stormApiService: StormApiService): Single<Boolean> {
    val credentials = JsonObject()
    credentials.addProperty("appname", "storm_app")
    credentials.addProperty("password", "C0R3MELTDOWN!")
    Timber.e("get resp")
    return stormApiService.appToken(credentials)
        .flatMap {
            Timber.e(it.toString())
            Single.just(
                if (!it.success) {
                    false
                } else {
                    Prefs.putString(PREF_APP_TOKEN, it.token)
                    true
                }
            )
        }
}

private fun sendSmSReq(message: String, number: String): Single<Any> {
    val map = JsonObject().apply {
        addProperty("from", "NetPlus")
        addProperty("to", "+234${number.substring(1)}")
        addProperty("message", message)
    }
    Timber.e("payload: $map")
    val auth = "Bearer ${Prefs.getString(PREF_USER_TOKEN, "")}"
    val body: RequestBody = map.toString()
        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    return StormApiClient.getSmsServiceInstance().sendSms(auth, body)
}

fun sendSMS(
    message: String,
    number: String,
    _smsSent: MutableLiveData<Event<Boolean>>,
    compositeDisposable: CompositeDisposable
) {
    val req = if (checkAppToken().not()) {
        Timber.e("app token not found, get it first")
        getAppToken(StormApiClient.getBillsInstance())
            .flatMap {
                sendSmSReq(message, number)
            }
    } else {
        sendSmSReq(message, number)
    }

    req
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { t1, t2 ->
            t1?.let {
                _smsSent.value = Event(true)
                Timber.e("Data $it")
            }
            t2?.let {
                Timber.e(it)
                val httpException = it as? HttpException
                httpException?.let { e ->
                }
                // MqttHelper.sendPayload(MqttTopics.SMS_EVENTS, smsEvent)
                _smsSent.value = Event(false)
            }
        }.disposeWith(compositeDisposable)
}
