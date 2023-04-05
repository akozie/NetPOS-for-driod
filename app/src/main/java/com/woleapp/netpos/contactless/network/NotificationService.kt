package com.woleapp.netpos.contactless.network

import com.woleapp.netpos.contactless.model.*
import com.woleapp.netpos.contactless.util.Resource
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface NotificationService {

    @POST("registerDeviceToken")
    fun registerDeviceToken(
        @Body notificationRegisterDeviceTokenModel: NotificationRegisterDeviceTokenModel
    ): Single<RegisterDeviceTokenResponse>
}