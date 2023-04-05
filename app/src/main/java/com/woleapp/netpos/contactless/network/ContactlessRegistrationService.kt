package com.woleapp.netpos.contactless.network

import com.woleapp.netpos.contactless.model.RegistrationModel
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface ContactlessRegistrationService {
    @POST("user/register")
    fun register(
        @Body registrationModel: RegistrationModel?,
        @Query("deviceSerialId") deviceSerialId: String
        ): Single<RegistrationModel>
}


object ContactlessClient {
    private const val contactlessBaseUrl = "https://contactless.netpluspay.com/"
    var contactlessClientInstance: ContactlessRegistrationService? = null
    fun getContactlessService(): ContactlessRegistrationService =
        contactlessClientInstance ?: synchronized(this) {
            Retrofit.Builder()
                .baseUrl(contactlessBaseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(
                    OkHttpClient.Builder()
                        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                        .build()
                )
                .build()
                .create(ContactlessRegistrationService::class.java).also {
                    contactlessClientInstance = it
                }
        }

}