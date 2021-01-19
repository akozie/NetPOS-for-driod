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

        private const val NIP_BASE_URL = "https://storm.netpluspay.com/"
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

        private var smsServiceInstance: SmsService? = null
        fun getSmsServiceInstance(): SmsService = smsServiceInstance ?: synchronized(this) {
            smsServiceInstance ?: Retrofit.Builder()
                .baseUrl("https://sms.netpluspay.com")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(SmsService::class.java)
                .also {
                    smsServiceInstance = it
                }
        }

        private var masterPassQRServiceInstance: MasterPassQRService? = null
        fun getMasterPassQrServiceInstance(): MasterPassQRService = masterPassQRServiceInstance ?: synchronized(this){
             masterPassQRServiceInstance ?: Retrofit.Builder()
                 .baseUrl("https://masterpassqr.netpluspay.com/api/v1/")
                 .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                 .addConverterFactory(GsonConverterFactory.create())
                 .build()
                 .create(MasterPassQRService::class.java)
                 .also {
                     masterPassQRServiceInstance = it
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
        Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java).netplus_id
        Prefs.getString(PREF_USER_TOKEN, "")
        builder.addHeader("X-CLIENT-ID", "85522f45-e459-4548-8b20-3a922196c515")
        builder.addHeader("X-ACCESSCODE",
            "a14014e18e2cffc4d74e150ed68a472bd94189db82d374306d5b307dc7620f20"

        )
        return chain.proceed(builder.build())
    }
}