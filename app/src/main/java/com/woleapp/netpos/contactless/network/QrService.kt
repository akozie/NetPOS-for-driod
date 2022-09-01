package com.woleapp.netpos.contactless.network

import com.google.gson.JsonObject
import com.woleapp.netpos.contactless.model.PostQrToServerModel
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface QrService {
    @POST("qr/processPayment")
    fun sendQrToServer(@Body sendQrPayLoad: PostQrToServerModel): Single<Response<JsonObject>>
}
