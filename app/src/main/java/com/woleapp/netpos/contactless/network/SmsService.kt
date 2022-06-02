package com.woleapp.netpos.contactless.network

import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SmsService {
    @POST("sms")
    fun sendSms(@Header("Authorization") header: String, @Body payload: RequestBody): Single<Any>
}