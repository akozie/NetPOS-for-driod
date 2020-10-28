package com.woleapp.netpos.network

import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.util.PREF_USER_TOKEN
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class StormApiClient {

    companion object {
        private fun getOkHttpClient() =
            OkHttpClient.Builder()
                .addInterceptor(TokenInterceptor())
                .build()

        private const val BASE_URL = "https://storm.netpluspay.com/"
        private var INSTANCE: StormApiService? = null
        fun getInstance(): StormApiService = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(StormApiService::class.java)
                .also {
                    INSTANCE = it
                }
        }
    }
}

class TokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val headersInReq = request.headers()
        if (headersInReq.get("Authorization").isNullOrEmpty()) {
            Prefs.getString(PREF_USER_TOKEN, null)?.let {
                request = request.newBuilder().addHeader("Authorization", "Bearer $it").build()
            }
        }
        val response = chain.proceed(request)
        val body = response.body()
        val bodyString = body?.string()
        Timber.e("resp: $bodyString")
        return response.newBuilder().body(ResponseBody.create(body!!.contentType(), bodyString!!))
            .build()
    }
}