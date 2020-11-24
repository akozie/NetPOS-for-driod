package com.woleapp.netpos.network

import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.User
import com.woleapp.netpos.util.PREF_USER
import com.woleapp.netpos.util.PREF_USER_TOKEN
import com.woleapp.netpos.util.Singletons
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

        private fun getNipOkHttpClient() = OkHttpClient.Builder()
            .addInterceptor(NipInterceptor())
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

        private const val NIP_BASE_URL = "http://storm.test.netpluspay.com/"
        private var NIPINSTANCE: NipService? = null
        fun getNipInstance(): NipService = NIPINSTANCE ?: synchronized(this) {
            NIPINSTANCE ?: Retrofit.Builder()
                .baseUrl(NIP_BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(getNipOkHttpClient())
                .build()
                .create(NipService::class.java)
                .also {
                    NIPINSTANCE = it
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

class NipInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
        val userId =
            Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java).netplus_id
        val userToken = Prefs.getString(PREF_USER_TOKEN, "")
        builder.addHeader("X-CLIENT-ID", "d84d4cba-2d14-4ceb-944f-a0a48344b768")
        builder.addHeader("X-ACCESSCODE", "78b4311d3e83a85c924edcc60dddc99d76fde9a21459e879fcd88878a31de8c5")
        return chain.proceed(builder.build())
    }
}