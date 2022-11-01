package com.woleapp.netpos.contactless.di.appModule

import com.google.gson.Gson
import com.woleapp.netpos.contactless.BuildConfig
import com.woleapp.netpos.contactless.network.AccountLookUpService
import com.woleapp.netpos.contactless.network.QrService
import com.woleapp.netpos.contactless.network.VerveOtpService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @Named("defaultBaseUrl")
    fun providesBaseUrlForGettingThreshold(): String = BuildConfig.STRING_GET_QR_BASE_URL

    @Provides
    @Singleton
    @Named("otpBaseUrl")
    fun providesBaseUrlForGetVerveOtp(): String = BuildConfig.STRING_SEND_VERVE_OTP_BASE_URL

    @Provides
    @Singleton
    @Named("contactlessRegBaseUrl")
    fun providesBaseUrlForContactlessReg(): String = BuildConfig.STRING_CONTACTLESS_EXISTING_BASE_URL

    @Provides
    @Singleton
    @Named("loggingInterceptor")
    fun providesLoggingInterceptor(): Interceptor = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    @Provides
    @Singleton
    @Named("headerInterceptor")
    fun providesHeaderInterceptor(): Interceptor =
        BasicAuthInterceptor(BuildConfig.STRING_AUTH_USER_NAME, BuildConfig.STRING_AUTH_PASSWORD)

    @Provides
    @Singleton
    @Named("defaultOkHttp")
    fun providesOKHTTPClient(
        @Named("loggingInterceptor") loggingInterceptor: Interceptor,
        @Named("headerInterceptor") headerInterceptor: Interceptor
    ): OkHttpClient =
        OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

    @Provides
    @Singleton
    @Named("otpOkHttp")
    fun providesOKHTTPClientForGetVerveOtp(
        @Named("loggingInterceptor") loggingInterceptor: Interceptor
    ): OkHttpClient =
        OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)
            .build()

    @Provides
    @Singleton
    @Named("defaultRetrofit")
    fun providesRetrofit(
        @Named("defaultOkHttp") okhttp: OkHttpClient,
        @Named("defaultBaseUrl") baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(baseUrl)
            .client(okhttp)
            .build()

    @Provides
    @Singleton
    @Named("otpRetrofit")
    fun providesRetrofitForVerveOtp(
        @Named("otpOkHttp") okhttp: OkHttpClient,
        @Named("otpBaseUrl") otpBaseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(otpBaseUrl)
            .client(okhttp)
            .build()

    @Provides
    @Singleton
    @Named("contactlessRegRetrofit")
    fun providesRetrofitForContactlessReg(
        @Named("otpOkHttp") okhttp: OkHttpClient,
        @Named("contactlessRegBaseUrl") contactlessRegBaseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(contactlessRegBaseUrl)
            .client(okhttp)
            .build()

    @Provides
    @Singleton
    fun providesQrService(
        @Named("defaultRetrofit") retrofit: Retrofit
    ): QrService = retrofit.create(QrService::class.java)

    @Provides
    @Singleton
    fun providesVerveOtpService(
        @Named("otpRetrofit") retrofit: Retrofit
    ): VerveOtpService = retrofit.create(VerveOtpService::class.java)


    @Provides
    @Singleton
    fun providesContactlessRegService(
        @Named("contactlessRegRetrofit") retrofit: Retrofit
    ): AccountLookUpService = retrofit.create(AccountLookUpService::class.java)


    @Provides
    @Singleton
    fun providesGson(): Gson = Gson()
}
