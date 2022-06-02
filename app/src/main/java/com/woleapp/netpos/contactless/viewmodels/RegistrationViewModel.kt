package com.woleapp.netpos.contactless.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.woleapp.netpos.contactless.model.RegistrationError
import com.woleapp.netpos.contactless.model.RegistrationModel
import com.woleapp.netpos.contactless.network.ContactlessClient
import com.woleapp.netpos.contactless.util.Event
import com.woleapp.netpos.contactless.util.Singletons
import com.woleapp.netpos.contactless.util.disposeWith
import com.woleapp.netpos.contactless.util.getResponseBody
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class RegistrationViewModel : ViewModel() {

    val authInProgress = MutableLiveData(false)
    private val contactlessService = ContactlessClient.getContactlessService()
    private val disposable = CompositeDisposable()

    val registrationModel: MutableLiveData<RegistrationModel> = MutableLiveData(
        RegistrationModel()
    )

    private val _message = MutableLiveData<Event<String>>()
    private val _authDone = MutableLiveData<Event<Boolean>>()
    val authDone: LiveData<Event<Boolean>>
        get() = _authDone

    val message: LiveData<Event<String>>
        get() = _message


    fun register() {
        if (registrationModel.value?.allFieldsFilled() == false) {
            _message.value = Event("All fields are required")
            return
        }
        authInProgress.value = true
        contactlessService.register(registrationModel.value)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                authInProgress.value = false
            }
            .subscribe { t1, t2 ->
                t1?.let {
                    registrationModel.value = RegistrationModel()
                    _authDone.value = Event(true)
                }
                t2?.let {
                    val errorMessage = try {
                        Singletons.gson.fromJson(
                            it.getResponseBody(),
                            RegistrationError::class.java
                        ).message
                    } catch (e: Exception) {
                        "an error occurred during registration, try again or contact support"
                    }
                    _message.value =
                        Event(errorMessage)
                }
            }.disposeWith(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }

    fun setSelectedBank(s: String) {
        registrationModel.value = registrationModel.value?.apply {
            bank = s
        }
    }
}