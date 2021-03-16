package com.woleapp.netpos.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs

import com.woleapp.netpos.network.StormApiService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.lang.Exception

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
                }else{
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