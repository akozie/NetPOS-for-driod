package com.woleapp.netpos.contactless.network

import com.google.gson.JsonObject
import com.woleapp.netpos.contactless.model.PostQrToServerModel
import com.woleapp.netpos.contactless.model.SendOtpForVerveCardModel
import io.reactivex.Single
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    private val qrService: QrService,
    private val otpService: VerveOtpService
) {
    fun postScannedQrRequestToServer(qrData: PostQrToServerModel): Single<Response<JsonObject>> =
        qrService.sendQrToServer(qrData)

    fun consummateTransactionBySendingOtp(otpPayLoad: SendOtpForVerveCardModel): Single<Response<JsonObject>> =
        otpService.sendOtpForVerveCard(otpPayLoad)
}
