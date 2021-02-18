package com.woleapp.netpos.network

import com.google.gson.JsonObject
import com.woleapp.netpos.model.NibssQRResponse
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

interface MasterPassQRService {
    @POST("qr/static")
    fun getStaticQr(@Body body: JsonObject):Single<ResponseBody>
}

interface NibssQRService{
    @POST("getQR")
    fun getQr(@Body body: JsonObject): Single<NibssQRResponse>

    @POST("queryTransactionStatus")
    fun queryTransactionStatus(@Body body: JsonObject): Single<NibssQRResponse>
}