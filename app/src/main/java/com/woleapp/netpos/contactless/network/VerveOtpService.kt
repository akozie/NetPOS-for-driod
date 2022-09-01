package com.woleapp.netpos.contactless.network

import com.google.gson.JsonObject
import com.woleapp.netpos.contactless.model.SendOtpForVerveCardModel
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface VerveOtpService {
    @POST("v2/pay")
    fun sendOtpForVerveCard(@Body sendOtpPayLoad: SendOtpForVerveCardModel): Single<Response<JsonObject>>
}
