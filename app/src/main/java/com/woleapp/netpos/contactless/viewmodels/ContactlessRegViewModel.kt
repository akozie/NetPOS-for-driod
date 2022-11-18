package com.woleapp.netpos.contactless.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.woleapp.netpos.contactless.model.AccountNumberLookUpResponse
import com.woleapp.netpos.contactless.model.ConfirmOTPResponse
import com.woleapp.netpos.contactless.model.ExistingAccountRegisterRequest
import com.woleapp.netpos.contactless.model.ExistingAccountRegisterResponse
import com.woleapp.netpos.contactless.network.ContactlessRegRepository
import com.woleapp.netpos.contactless.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactlessRegViewModel @Inject constructor(
    private val contactlessRegRepo: ContactlessRegRepository
) : ViewModel() {
    private val disposable = CompositeDisposable()

    private var _accountNumberResponse: MutableLiveData<Resource<AccountNumberLookUpResponse>> =
        MutableLiveData()
    val accountNumberResponse: LiveData<Resource<AccountNumberLookUpResponse>> get() = _accountNumberResponse


    private var _confirmOTPResponse: MutableLiveData<Resource<ConfirmOTPResponse>> =
        MutableLiveData()
    val confirmOTPResponse: LiveData<Resource<ConfirmOTPResponse>> get() = _confirmOTPResponse


    private var _existingRegRequestResponse: MutableLiveData<Resource<ExistingAccountRegisterResponse>> =
        MutableLiveData()
    val existingRegRequestResponse: LiveData<Resource<ExistingAccountRegisterResponse>> get() = _existingRegRequestResponse


    fun accountLookUp(accountNumber: String) {
        _accountNumberResponse.postValue(Resource.loading(null))
        disposable.add(
            contactlessRegRepo.findAccount(accountNumber)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let {
                        _accountNumberResponse.postValue(Resource.success(it))
                    }
                    error?.let {
                        _accountNumberResponse.postValue(Resource.error(null))
                        Timber.d("ERROR==${it.localizedMessage}")
                    }
                }
        )
    }

    fun confirmOTP(accountNumber: String, otp: String) {
        _confirmOTPResponse.postValue(Resource.loading(null))
        disposable.add(
            contactlessRegRepo.confirmOTP(otp, accountNumber)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let {
                        _confirmOTPResponse.postValue(Resource.success(it))
                    }
                    error?.let {
                        _confirmOTPResponse.postValue(Resource.error(null))
                        Timber.d("ERROR==${it.localizedMessage}")
                    }
                }
        )
    }

    fun registerExistingAccount(accountNumber: ExistingAccountRegisterRequest, bank: String, partnerId:String) {
        _existingRegRequestResponse.postValue(Resource.loading(null))
        disposable.add(
            contactlessRegRepo.registerExistingAccount(accountNumber, bank, partnerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let {
                        _existingRegRequestResponse.postValue(Resource.success(it))
                    }
                    error?.let {
                        _existingRegRequestResponse.postValue(Resource.error(null))
                        Timber.d("ERROR==${it.localizedMessage}")
                    }
                }
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}