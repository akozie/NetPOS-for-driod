package com.woleapp.netpos.viewmodels

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.network.StormApiService
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.Singletons.gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import timber.log.Timber

class AuthViewModel : ViewModel() {
    private val disposables = CompositeDisposable()
    var stormApiService: StormApiService? = null
    var appCredentials: JsonObject? = null
    val authInProgress = MutableLiveData(false)
    val passwordResetInProgress = MutableLiveData(false)
    val usernameLiveData = MutableLiveData("")
    val passwordLiveData = MutableLiveData("")
    private val _message = MutableLiveData<Event<String>>()
    private val _authDone = MutableLiveData<Event<Boolean>>()
    private val _gotoAdminPage = MutableLiveData<Event<Boolean>>()
    private val _passwordResetSent = MutableLiveData<Event<Boolean>>()

    val passwordResetSent: LiveData<Event<Boolean>>
        get() = _passwordResetSent

    val gotoAdminPage: LiveData<Event<Boolean>>
        get() = _gotoAdminPage

    val authDone: LiveData<Event<Boolean>>
        get() = _authDone

    val message: LiveData<Event<String>>
        get() = _message


    private fun getAppToken(next: () -> Unit) {
        authInProgress.value = true
        stormApiService!!.appToken(appCredentials)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { res, error ->
                res?.let {
                    if (it.success) {
                        Prefs.putString(PREF_APP_TOKEN, it.token)
                        next.invoke()
                    } else {
                        authInProgress.value = false
                        _message.value = Event("An unexpected error occurred")
                    }
                }
                error?.let {
                    Timber.e(it)
                    authInProgress.value = false
                    _message.value = Event(it.localizedMessage ?: "authentication errorr")
                }
            }.disposeWith(disposables)
    }


    fun login() {
        val username = usernameLiveData.value
        val password = passwordLiveData.value
        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            _message.value = Event("All fields are required")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            _message.value = Event("Please enter a valid email")
            return
        }
        val appToken = Prefs.getString(PREF_APP_TOKEN, null)
        if (appToken == null || JWTHelper.isExpired(appToken)) {
            getAppToken(::login)
            return
        }
        auth(appToken, username, password)
    }

    private fun auth(appToken: String, username: String, password: String) {
        authInProgress.value = true
        val credentials = JsonObject()
            .apply {
                addProperty("username", username)
                addProperty("password", password)
            }
        stormApiService!!.userToken("Bearer $appToken", credentials)
            .flatMap {
                Timber.e(it.toString())
                if (BuildConfig.BUILD_TYPE.equals(
                        "releaseAdmin",
                        true
                    ) || BuildConfig.BUILD_TYPE.equals("nibssserverdebug", true)
                )
                    if (username == "dapo@webmallng.com") {
                        //raise a fake exception to stop the process
                        throw Exception("ADMIN")
                    }
                if (!it.success) {
                    throw Exception("Login Failed, Check Credentials")
                }
                val userToken = it.token
                val stormId: String =
                    JWTHelper.getStormId(userToken) ?: throw Exception("Login Failed")
                Prefs.putString(PREF_USER_TOKEN, userToken)
                stormApiService!!.getAgentDetails(stormId)
            }.subscribeOn(Schedulers.io())
            .doFinally { authInProgress.postValue(false) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { res, error ->
                res?.let {
                    Prefs.putString(PREF_USER, gson.toJson(it))
                    Prefs.putBoolean(PREF_AUTHENTICATED, true)
                    _authDone.value = Event(true)
                }
                error?.let {
                    Timber.e(it)
                    if (it.message.equals("admin", true)) {
                        _gotoAdminPage.value = Event(true)
                        return@let
                    }
                    Timber.e(it.localizedMessage)
                    _message.value = Event(it.localizedMessage ?: "login error")
                }
            }.disposeWith(disposables)

    }

    fun resetPassword() {
        val username = usernameLiveData.value
        if (username.isNullOrEmpty()) {
            _message.value = Event("Please enter your email address")
            return
        }
        val appToken = Prefs.getString(PREF_APP_TOKEN, null)
        if (appToken == null || JWTHelper.isExpired(appToken)) {
            getAppToken(::resetPassword)
            return
        }
        val payload = JsonObject().apply {
            addProperty("username", username)
        }
        stormApiService!!.passwordReset(appToken, payload).subscribeOn(Schedulers.io())
            .doOnSubscribe {
                passwordResetInProgress.postValue(true)
            }.doFinally { passwordResetInProgress.postValue(false) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    _message.value = if (it.code() != 200) {
                        Event("Password reset failed")
                    } else {
                        val res = JSONObject(Gson().toJson(it.body()))
                        if (!res.getBoolean("success"))
                            Event("Password reset failed")
                        else {
                            _passwordResetSent.value = Event(true)
                            Event("A password reset mail has been sent to $username")
                        }
                    }
                }
                t2?.let {
                    _message.value = Event("Password reset failed, try again.")
                }
            }.disposeWith(disposables)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}