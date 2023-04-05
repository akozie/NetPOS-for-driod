package com.woleapp.netpos.contactless.viewmodels

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.woleapp.netpos.contactless.BuildConfig
import com.woleapp.netpos.contactless.R
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
import kotlinx.android.synthetic.main.dialog_terms_and_conditions.view.*

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


    fun register(context:Context, deviceSerialId: String) {
        if (BuildConfig.FLAVOR.contains("providus") ||
            BuildConfig.FLAVOR.contains("fcmb")|| BuildConfig.FLAVOR.contains("fcmbeasypay")||
            BuildConfig.FLAVOR.contains("easypay")) {
//            activity?.getFragmentManager()?.popBackStack()
            if (registrationModel.value?.allFieldsFilled() == false) {
                _message.value = Event("All fields are required")
                return
            }else {
                val dialogView: View = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_terms_and_conditions, null)
                val dialogBuilder: AlertDialog.Builder =
                    AlertDialog.Builder(context)
                dialogBuilder.setView(dialogView)

                val alertDialog: AlertDialog = dialogBuilder.create()
                alertDialog.show()
                if (BuildConfig.FLAVOR.contains("providus")) {
                    dialogView.pdf.fromAsset("providus.pdf").load()
                } else if (BuildConfig.FLAVOR.contains("fcmb")) {
                    dialogView.pdf.fromAsset("qlick.pdf").load()
                } else if (BuildConfig.FLAVOR.contains("easypay")) {
                    dialogView.pdf.fromAsset("qlick.pdf").load()
                }else if (BuildConfig.FLAVOR.contains("fcmbeasypay")) {
                    dialogView.pdf.fromAsset("qlick.pdf").load()
                }
                dialogView.accept_button.setOnClickListener {
                    alertDialog.dismiss()
                    reg(deviceSerialId)
                }
            }
        }else{
            if (registrationModel.value?.allFieldsFilled() == false) {
                _message.value = Event("All fields are required")
                return
            }else{
                reg(deviceSerialId)
            }
        }
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

    private fun reg(deviceSerialId: String){
        authInProgress.value = true
        contactlessService.register(registrationModel.value, deviceSerialId)
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
}