package com.woleapp.netpos.contactless.network

import com.woleapp.netpos.contactless.model.AccountNumberLookUpRequest
import com.woleapp.netpos.contactless.model.NotificationRegisterDeviceTokenModel
import com.woleapp.netpos.contactless.model.PayWithQrRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationService: NotificationService
) {

    fun registerDeviceToken(deviceRegistrationToken: String,
                             terminalId: String,
                             username: String) = notificationService.registerDeviceToken(
        NotificationRegisterDeviceTokenModel(deviceRegistrationToken, terminalId, username))

}