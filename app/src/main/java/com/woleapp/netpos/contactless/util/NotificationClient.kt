package com.woleapp.netpos.contactless.util

import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.network.NotificationService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber


class NotificationClient {
    companion object {

        private fun getBaseOkhttpClientBuilder(): OkHttpClient.Builder {
            val okHttpClientBuilder = OkHttpClient.Builder()

            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(loggingInterceptor)

            return okHttpClientBuilder
        }

        private fun getOkHttpClient() =
            getBaseOkhttpClientBuilder()
                .addInterceptor(TokenInterceptor())
                .build()

        private const val BASE_URL = "https://netpos.netpluspay.com/api/"
        private var INSTANCE: NotificationService? = null
        fun getInstance(): NotificationService = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NotificationService::class.java)
                .also {
                    INSTANCE = it
                }
        }
    }
}

class TokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val headersInReq = request.headers
        if (headersInReq["Authorization"].isNullOrEmpty()) {
            Prefs.getString(PREF_USER_TOKEN, null)?.let {
                request = request.newBuilder().addHeader("Authorization", "Bearer $it").build()
            }
        }
        val response = chain.proceed(request)
        val body = response.body
        val bodyString = body?.string()
        Timber.e("resp: $bodyString")
        return response.newBuilder().body(bodyString!!.toResponseBody(body.contentType()))
            .build()
    }
}
