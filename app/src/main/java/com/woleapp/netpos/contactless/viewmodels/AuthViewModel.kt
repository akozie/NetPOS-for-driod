package com.woleapp.netpos.contactless.viewmodels

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.auth0.android.jwt.JWT
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.model.AuthError
import com.woleapp.netpos.contactless.model.User
import com.woleapp.netpos.contactless.network.StormApiService
import com.woleapp.netpos.contactless.util.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.contactless.util.Singletons.gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import retrofit2.HttpException
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
        auth(username, password)
    }

    private fun auth(username: String, password: String) {
        authInProgress.value = true
        val credentials = JsonObject()
            .apply {
                addProperty("username", username)
                addProperty("password", password)
            }
        stormApiService!!.userToken(credentials)
            .flatMap {
                Timber.e(it.toString())
                if (!it.success) {
                    throw Exception("Login Failed, Check Credentials")
                }
                val userToken = it.token
                val stormId: String =
                    JWTHelper.getStormId(userToken) ?: throw Exception("Login Failed")
                Prefs.putString(PREF_USER_TOKEN, userToken)
                val userTokenDecoded = JWT(userToken)
                val user = User().apply {
                    this.terminal_id =
                        if (userTokenDecoded.claims.containsKey("terminalId")) userTokenDecoded.getClaim(
                            "terminalId"
                        ).asString() else null
                    this.business_name =
                        if (userTokenDecoded.claims.containsKey("businessName")) userTokenDecoded.getClaim(
                            "businessName"
                        ).asString() else null
                    this.netplus_id =
                        if (userTokenDecoded.claims.containsKey("stormId")) userTokenDecoded.getClaim(
                            "stormId"
                        ).asString() else null
                    this.mid =
                        if (userTokenDecoded.claims.containsKey("mid")) userTokenDecoded.getClaim("mid")
                            .asString() else null
                    this.partnerId =
                        if (userTokenDecoded.claims.containsKey("partnerId")) userTokenDecoded.getClaim(
                            "partnerId"
                        ).asString() else null
                }
                Timber.e(user.terminal_id)
                Single.just(user)
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
                    (it as? HttpException).let { httpException ->
                        val errorMessage = httpException?.response()?.errorBody()?.string()
                            ?: "{\"success\":false,\"message\":\"Unexpected error\"}"
                        _message.value = Event(
                            try {
                                gson.fromJson(errorMessage, AuthError::class.java).message
                                    ?: "Login Failed"
                            } catch (e: Exception) {
                                "login failed"
                            }
                        )
                        Timber.e(errorMessage)
                    }
                }
            }.disposeWith(disposables)
    }

    fun resetPassword() {
        val username = usernameLiveData.value
        if (username.isNullOrEmpty()) {
            _message.value = Event("Please enter your email address")
            return
        }

        val payload = JsonObject().apply {
            addProperty("username", username)
        }
        stormApiService!!.passwordReset(payload).subscribeOn(Schedulers.io())
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
                        if (!res.getBoolean("success")) {
                            Event("Password reset failed")
                        } else {
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
