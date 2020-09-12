package com.woleapp.netpos.network

import com.google.gson.JsonObject
import com.woleapp.netpos.model.NipNotification
import com.woleapp.netpos.model.SessionCode
import com.woleapp.netpos.model.TokenResp
import com.woleapp.netpos.model.User
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.*

interface StormApiService {
    @POST("api/token")
    fun appToken(@Body credentials: JsonObject?): Single<TokenResp>

    @POST("api/auth")
    fun userToken(
        @Header("Authorization") appToken: String?,
        @Body credentials: JsonObject?
    ): Single<TokenResp>

    @GET("api/agents/{stormId}")
    fun getAgentDetails(@Path("stormId") stormId: String?): Single<User>

    @POST("api/passwordReset")
    fun passwordReset(
        @Header("Authorization") appToken: String?,
        @Body payload: JsonObject?
    ): Single<Response<Any?>?>

    @GET("/api/nip-notifications")
    fun getNotificationByReference(
        @Query("referenceNo") reference: String,
        @Header("X-CLIENT-ID") clientId: String,
        @Header("X-ACCESSCODE") accessCode: String
    ): Single<NipNotification>

    @GET("/api/nip-notifications")
    fun getNotifications(
        @Query("terminalId") terminalId: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Header("X-CLIENT-ID") clientId: String,
        @Header("X-ACCESSCODE") accessCode: String
    ): Single<List<NipNotification>>

    @GET("api/sessionCode")
    fun getSessionCode(): Single<SessionCode>
}