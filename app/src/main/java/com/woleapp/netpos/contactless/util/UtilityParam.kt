package com.woleapp.netpos.contactless.util

object UtilityParam {

    init {
        System.loadLibrary("api-keys")
    }

    private external fun getAuthUserName(): String
    private external fun getAuthPassword(): String
    private external fun getGetQrBaseUrl(): String
    private external fun getSendVerveOtpBaseUrl(): String
    private external fun getMerchantId(): String
    private external fun getWebViewBaseUrl(): String
    private external fun getContactlessExistingBaseUrl():String
    private external fun getContactlessPaymentWithQrBaseUrl():String
    private external fun getNotificationBaseUrl():String
    private external fun getMpgsTag(): String

    val STRING_AUTH_USER_NAME=getAuthUserName()
    val STRING_AUTH_PASSWORD=getAuthPassword()
    val STRING_GET_QR_BASE_URL=getGetQrBaseUrl()
    val STRING_SEND_VERVE_OTP_BASE_URL=getSendVerveOtpBaseUrl()
    val STRING_MERCHANT_ID=getMerchantId()
    val STRING_WEB_VIEW_BASE_URL= getWebViewBaseUrl()
    val STRING_CONTACTLESS_EXISTING_BASE_URL= getContactlessExistingBaseUrl()
    val STRING_CONTACTLESS_PAYMENT_WITH_QR_BASE_URL= getContactlessPaymentWithQrBaseUrl()
    val STRING_NOTIFICATION_BASE_URL= getNotificationBaseUrl()
    val STRING_MPGS_TAG= getMpgsTag()
}