package com.woleapp.netpos.contactless.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.model.*
import com.woleapp.netpos.contactless.network.ContactlessRegRepository
import com.woleapp.netpos.contactless.util.AppConstants
import com.woleapp.netpos.contactless.util.Resource
import com.woleapp.netpos.contactless.util.Singletons.gson
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


    fun accountLookUp(accountNumber: String, partnerId: String) {
        _accountNumberResponse.postValue(Resource.loading(null))
        saveAccountNumber(accountNumber)
        disposable.add(
            contactlessRegRepo.findAccount(accountNumber, partnerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let {
                        _accountNumberResponse.postValue(Resource.success(it))
                        savePhoneNumber(data.phone)
                    }
                    error?.let {
                        _accountNumberResponse.postValue(Resource.error(null))
                        Timber.d("ERROR==${it.localizedMessage}")
                    }
                }
        )
    }

    fun confirmOTP(phoneNumber: String, otp: String, ) {
        _confirmOTPResponse.postValue(Resource.loading(null))
        disposable.add(
            contactlessRegRepo.confirmOTP(phoneNumber, otp)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let {
                        _confirmOTPResponse.postValue(Resource.success(it))
                        //saveExistingCustomerData(data.data)
                        saveExistingPhoneNumber(data.data.phone)
                        saveEmail(data.data.email)
                        saveBusinessName(data.data.businessName)
                        saveBusinessAddress(data.data.address)
                        saveFullName(data.data.fullName)
                    }
                    error?.let {
                        _confirmOTPResponse.postValue(Resource.error(null))
                        Timber.d("ERROR==${it.localizedMessage}")
                    }
                }
        )
    }

    fun registerExistingAccount(accountNumber: ExistingAccountRegisterRequest, partnerId:String) {
        _existingRegRequestResponse.postValue(Resource.loading(null))
        disposable.add(
            contactlessRegRepo.registerExistingAccount(accountNumber, partnerId)
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
    private fun savePhoneNumber(phoneNumber: String) {
        Prefs.putString(AppConstants.SAVE_PHONE_NUMBER, gson.toJson(phoneNumber))
    }

    private fun saveAccountNumber(ActNumber: String) {
        Prefs.putString(AppConstants.SAVED_ACCOUNT_NUM_SIGNED_UP, gson.toJson(ActNumber))
    }

    private fun saveExistingCustomerData(customerData: Data) {
        Prefs.putString(AppConstants.SAVE_CUSTOMER_DATA, gson.toJson(customerData))
    }
    private fun saveBusinessName(businessName: String) {
        Prefs.putString(AppConstants.BUSINESS_NAME, gson.toJson(businessName))
    }
    private fun saveFullName(fullName: String) {
        Prefs.putString(AppConstants.FULL_NAME, gson.toJson(fullName))
    }
    private fun saveBusinessAddress(businessAddress: String) {
        Prefs.putString(AppConstants.BUSINESS_ADDRESS, gson.toJson(businessAddress))
    }
    private fun saveExistingPhoneNumber(phoneNumber: String) {
        Prefs.putString(AppConstants.PHONE_NUMBER, gson.toJson(phoneNumber))
    }
    private fun saveEmail(email: String) {
        Prefs.putString(AppConstants.EMAIL_ADDRESS, gson.toJson(email))
    }

//    fun retrieveExistingCustomerData(): Data? {
//        val payload = Prefs.getString(AppConstants.SAVE_CUSTOMER_DATA, "")
//        return if (!payload.isNullOrEmpty()) {
//            gson.fromJson(payload, Data::class.java)
//        } else null
//    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}