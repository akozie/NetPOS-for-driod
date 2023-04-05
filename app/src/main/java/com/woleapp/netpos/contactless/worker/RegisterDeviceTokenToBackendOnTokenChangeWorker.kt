package com.woleapp.netpos.contactless.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woleapp.netpos.contactless.model.NotificationRegisterDeviceTokenModel
import com.woleapp.netpos.contactless.model.RegisterDeviceTokenResponse
import com.woleapp.netpos.contactless.util.AppConstants.NOTIFICATION_ERROR
import com.woleapp.netpos.contactless.util.AppConstants.WORKER_INPUT_FIREBASE_DEVICE_TOKEN_TAG
import com.woleapp.netpos.contactless.util.NotificationClient
import com.woleapp.netpos.contactless.util.RxJavaUtils.getSingleTransformer
import com.woleapp.netpos.contactless.util.Singletons
import com.woleapp.netpos.contactless.util.disposeWith
import io.reactivex.disposables.CompositeDisposable

class RegisterDeviceTokenToBackendOnTokenChangeWorker(
    val context: Context,
    private val workParameters: WorkerParameters
) : Worker(context, workParameters) {
    private lateinit var compositeDisposable: CompositeDisposable

    private val notificationRepository = NotificationClient.getInstance()


    override fun doWork(): Result {
        val terminalId = Singletons.getCurrentlyLoggedInUser()?.terminal_id
        val userName = Singletons.getCurrentlyLoggedInUser()?.netplus_id.toString()
        val deviceToken = inputData.getString(WORKER_INPUT_FIREBASE_DEVICE_TOKEN_TAG)

        val response = terminalId?.let { tid ->
            var tokenResponse: RegisterDeviceTokenResponse? = null
            deviceToken?.let { token ->
                val req = NotificationRegisterDeviceTokenModel(token, tid, userName)
                notificationRepository.registerDeviceToken(req)
                    .compose(getSingleTransformer(NOTIFICATION_ERROR))
                    .subscribe { value ->
                        tokenResponse = value
                    }.disposeWith(compositeDisposable)
            }
            tokenResponse?.success
        } ?: false

        return if (response) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}