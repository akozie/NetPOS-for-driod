package com.woleapp.netpos.contactless.viewmodels

import androidx.lifecycle.ViewModel
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.model.PayWithQrRequest
import com.woleapp.netpos.contactless.network.ContactlessQrPaymentRepository
import com.woleapp.netpos.contactless.network.NotificationRepository
import com.woleapp.netpos.contactless.util.AppConstants
import com.woleapp.netpos.contactless.util.AppConstants.NOTIFICATION_ERROR
import com.woleapp.netpos.contactless.util.Resource
import com.woleapp.netpos.contactless.util.RxJavaUtils.getSingleTransformer
import com.woleapp.netpos.contactless.util.Singletons.gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val disposable: CompositeDisposable
) : ViewModel() {

    fun registerDeviceToken(token: String, terminalId: String,
                            username: String ) {
        disposable.add(
            notificationRepository.registerDeviceToken(token, terminalId, username)
                .compose(getSingleTransformer(NOTIFICATION_ERROR))
                .subscribe()
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}

