package com.woleapp.netpos.contactless.di.appModule

import com.google.gson.Gson
import com.woleapp.netpos.contactless.BuildConfig
import com.woleapp.netpos.contactless.network.*
import com.woleapp.netpos.contactless.util.UtilityParam
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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
    fun providesBaseUrlForGettingThreshold(): String = UtilityParam.STRING_GET_QR_BASE_URL

    @Provides
    @Singleton
    @Named("otpBaseUrl")
    fun providesBaseUrlForGetVerveOtp(): String = UtilityParam.STRING_SEND_VERVE_OTP_BASE_URL

    @Provides
    @Singleton
    @Named("contactlessRegBaseUrl")
    fun providesBaseUrlForContactlessReg(): String =
        UtilityParam.STRING_CONTACTLESS_EXISTING_BASE_URL

    @Provides
    @Singleton
    @Named("contactlessQrPaymentBaseUrl")
    fun providesBaseUrlForContactlessPaymentWithQr(): String =
        UtilityParam.STRING_CONTACTLESS_PAYMENT_WITH_QR_BASE_URL

    @Provides
    @Singleton
    @Named("notificationBaseUrl")
    fun providesBaseUrlForNotification(): String =
        UtilityParam.STRING_NOTIFICATION_BASE_URL

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
        BasicAuthInterceptor(UtilityParam.STRING_AUTH_USER_NAME, UtilityParam.STRING_AUTH_PASSWORD)

    @Provides
    @Singleton
    @Named("defaultOkHttp")
    fun providesOKHTTPClient(
        @Named("loggingInterceptor") loggingInterceptor: Interceptor,
        @Named("headerInterceptor") headerInterceptor: Interceptor
    ): OkHttpClient =
        OkHttpClient().newBuilder()
            .connectTimeout(70, TimeUnit.SECONDS)
            .readTimeout(70, TimeUnit.SECONDS)
            .writeTimeout(70, TimeUnit.SECONDS)
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
            .connectTimeout(70, TimeUnit.SECONDS)
            .readTimeout(70, TimeUnit.SECONDS)
            .writeTimeout(70, TimeUnit.SECONDS)
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
    @Named("contactlessQrPaymentRetrofit")
    fun providesRetrofitForContactlessQrPayment(
        @Named("otpOkHttp") okhttp: OkHttpClient,
        @Named("contactlessQrPaymentBaseUrl") contactlessQrPaymentBaseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(contactlessQrPaymentBaseUrl)
            .client(okhttp)
            .build()
    @Provides
    @Singleton
    @Named("notificationRetrofit")
    fun providesRetrofitForNotificationService(
        @Named("otpOkHttp") okhttp: OkHttpClient,
        @Named("notificationBaseUrl") notificationBaseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(notificationBaseUrl)
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
    fun providesContactlessQrPaymentService(
        @Named("contactlessQrPaymentRetrofit") retrofit: Retrofit
    ): QrPaymentService = retrofit.create(QrPaymentService::class.java)


    @Provides
    @Singleton
    fun providesNotificationService(
        @Named("notificationRetrofit") retrofit: Retrofit
    ): NotificationService = retrofit.create(NotificationService::class.java)


    @Provides
    @Singleton
    fun providesGson(): Gson = Gson()

    @Provides
    @Singleton
    @Named("io-scheduler")
    fun providesIoScheduler(): Scheduler = Schedulers.io()

    @Provides
    @Singleton
    @Named("main-scheduler")
    fun providesMainThreadScheduler(): Scheduler = AndroidSchedulers.mainThread()

    @Provides
    @Singleton
    fun providesCompositeDisposable(): CompositeDisposable = CompositeDisposable()
}
