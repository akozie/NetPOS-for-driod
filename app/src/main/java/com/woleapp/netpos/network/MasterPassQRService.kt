package com.woleapp.netpos.network

import com.google.gson.JsonObject
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

interface MasterPassQRService {
    @POST("qr/static")
    fun getStaticQr(@Body body: JsonObject):Single<ResponseBody>
}