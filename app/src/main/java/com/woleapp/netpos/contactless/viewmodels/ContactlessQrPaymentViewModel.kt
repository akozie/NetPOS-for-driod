package com.woleapp.netpos.contactless.viewmodels

import androidx.lifecycle.ViewModel
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.model.PayWithQrRequest
import com.woleapp.netpos.contactless.network.ContactlessQrPaymentRepository
import com.woleapp.netpos.contactless.util.AppConstants
import com.woleapp.netpos.contactless.util.Resource
import com.woleapp.netpos.contactless.util.Singletons.gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

@HiltViewModel
class ContactlessQrPaymentViewModel @Inject constructor(
    private val contactlessQrPaymentRepository: ContactlessQrPaymentRepository,
    private val disposable: CompositeDisposable
) : ViewModel() {


    fun paymentWithQr(payWithQrRequest: PayWithQrRequest) =
        contactlessQrPaymentRepository.payWithQr(payWithQrRequest)
            .flatMap {
                if (it.isSuccessful) {
                    savePaymentWithQrResponse(it.body().toString())
                    Single.just(Resource.success(it.body()))
                } else {
                    Single.just(Resource.error(it.errorBody()))
                }
            }

    private fun savePaymentWithQrResponse(data: String) {
        Prefs.putString(AppConstants.PAYMENT_WITH_QR_STRING, gson.toJson(data))
    }


    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}

