package com.woleapp.netpos.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.gson.JsonObject
import com.woleapp.netpos.model.*
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.network.ZenithQrMCCDataSourceFactory
import com.woleapp.netpos.util.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import retrofit2.HttpException
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class QRViewModel : ViewModel() {
    private var retryAttempts = 1
    var stillHasRetryAttempts = true
    private val masterPassQRService = StormApiClient.getMasterPassQrServiceInstance()
    private val nibssQRService = StormApiClient.getNibssQRServiceInstance()
    private val zenithQRService = StormApiClient.getZenithQRServiceInstance()
    private val disposable = CompositeDisposable()
    private val _masterPassQrBitmap = MutableLiveData<Event<Bitmap>>()
    val masterPassQrBitmap: LiveData<Event<Bitmap>>
        get() = _masterPassQrBitmap

    val message = MediatorLiveData<Event<String>>()

    private val _qrErrorMessage = MutableLiveData<Event<String>>()
    val qrErrorMessage: LiveData<Event<String>>
        get() = _qrErrorMessage

    private val lastNibssOrderNumber = MutableLiveData<String>()

    private val _nibssQRBitmap = MutableLiveData<Event<Bitmap>>()
    val nibssQRBitmap: LiveData<Event<Bitmap>>
        get() = _nibssQRBitmap

    private val _reQuerying = MutableLiveData<Event<Boolean>>()

    val reQuerying: LiveData<Event<Boolean>>
        get() = _reQuerying

    private val _createZenithMerchant = MutableLiveData<Event<Boolean>>()

    val createZenithMerchant: LiveData<Event<Boolean>>
        get() = _createZenithMerchant

    private val _zenithCityList = MutableLiveData<Event<List<ZenithCity>>>()

    val zenithCityList: LiveData<Event<List<ZenithCity>>>
        get() = _zenithCityList

    val createZenithMerchantPayload = MutableLiveData(CreateZenithMerchantPayload())

    val registrationInProgress = MutableLiveData(false)

    private val _paginationHelper = MutableLiveData<PaginationHelper>()
    private val emptyListLiveData = _paginationHelper.switchMap {
        it.emptyResultLiveData!!
    }

    init {
        message.addSource(emptyListLiveData) {
            message.value = Event("No results found")
        }
    }


    val loadingStateLiveData = _paginationHelper.switchMap {
        it.eventLiveData!!
    }

    private val _zenithQr = MutableLiveData<Event<Bitmap?>>()
    val zenithQr: LiveData<Event<Bitmap?>>
        get() = _zenithQr

    private val _zenithQrRegistrationDone = MutableLiveData<Event<Boolean>>()
    val zenithQrRegistrationDone: LiveData<Event<Boolean>>
        get() = _zenithQrRegistrationDone


    val zenithMccList = _paginationHelper.switchMap {
        Timber.e("size: ${it.data?.value?.size.toString()}")
        if (it.data == null) {
            Timber.e("data is null")
        }
        it.data!!
    }
    private val subject = PublishSubject.create<String>()

    private val config = PagedList.Config.Builder()
        .setInitialLoadSizeHint(20)
        .setPageSize(20)
        .setEnablePlaceholders(false)
        .build()


    fun getMasterPassQr(amount: Double) {
        Timber.e("Get masterpass")
        val qrRequestBody = JsonObject()
        val user = Singletons.getCurrentlyLoggedInUser()!!
        qrRequestBody.apply {
            addProperty("amount", amount.toString())
            addProperty("order_id", UUID.randomUUID().toString())
            addProperty("merchant_id", user.netplus_id)
            addProperty("currency_code", "NGN")
            addProperty("country_code", "NG")
            addProperty("business_name", user.business_name)
            addProperty("merchant_city", "Lagos")
        }
        masterPassQRService.getStaticQr(qrRequestBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                val bmp = BitmapFactory.decodeStream(it.byteStream())
                if (bmp != null)
                    Single.just(bmp)
                else
                    throw NullPointerException("Bitmap is null")
            }
            .subscribe { t1, t2 ->
                t1?.let { bmp ->
                    Timber.e("gotten and amount is $amount")
                    _masterPassQrBitmap.value = Event(bmp)
                }
                t2?.let { error ->
                    val httpException = error as? HttpException
                    httpException?.let {
                        Timber.e("body ${it.response()?.errorBody()?.string()}")
                        Timber.e("message ${it.message()}")
                        Timber.e(it.message ?: "Error")
                    }
                    _qrErrorMessage.value = Event("Error: ${error.localizedMessage ?: "Error"}")
                    message.value = Event(
                        "An error occurred while fetching QR"
                  )
                }
            }.disposeWith(disposable)
    }

    fun getNibssQR(amount: Double) {
        val start: Long = 111_111_111_111
        val end: Long = 999_999_999_999
        val range1 = (start..end).random()
        val range2 = (start..end).random()
        lastNibssOrderNumber.value =
            "${
                SimpleDateFormat(
                    "yMM",
                    Locale.getDefault()
                ).format(Date(System.currentTimeMillis()))
            }$range1$range2"
        val jsonObject = JsonObject()
        jsonObject.addProperty("amount", amount.toString())
        jsonObject.addProperty("order_no", lastNibssOrderNumber.value!!)
        nibssQRService.getQr(jsonObject)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                if (it.returnCode.isNullOrEmpty() || it.returnCode != "Success" || it.codeUrl.isNullOrEmpty())
                    throw Exception("Could not fetch QR code")
                Single.just(encodeAsBitmap(it.codeUrl, 150, 150))
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    _nibssQRBitmap.value = Event(it)
                    runHandler()
                }
                t2?.let {
                    message.value = Event("Error ${it.localizedMessage}")
                }
            }
            .disposeWith(disposable)
    }

    private fun runHandler() {
        if (retryAttempts > 15) {
            stillHasRetryAttempts = false
            message.value = Event("Too many attempts without response")
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            queryTransaction()
        }, 4000)
    }

    private fun queryTransaction() {
        retryAttempts += 1
        _reQuerying.value = Event(true)
        lastNibssOrderNumber.value?.let {
            val jsonObject = JsonObject()
            jsonObject.addProperty("order_no", it)
            nibssQRService.queryTransactionStatus(jsonObject)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _reQuerying.value = Event(false) }
                .subscribe { t1, t2 ->
                    t1?.let { nibssQRResponse ->
                        when (nibssQRResponse.returnCode) {
                            "Paying" -> runHandler()
                            "Success" -> {
                                stillHasRetryAttempts = false
                                message.value = Event("Success, payment confirmed")
                            }
                            else -> {
                                message.value = Event("Failed, payment failed")
                                stillHasRetryAttempts = false
                            }
                        }
                    }
                    t2?.let {
                        retryAttempts -= 1
                        message.value = Event("Retrying")
                    }
                }.disposeWith(disposable)
        }
    }

    fun getZenithQR() {
        zenithQRService.getZenithQr()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                val bitmap: Bitmap? = it.qrCode.decodeBase64ToBitmap()
                if (bitmap != null)
                    Single.just(bitmap)
                else
                    throw NullPointerException("Bitmap is null")
            }
            .subscribe { t1, t2 ->
                t1?.let {
                    _zenithQr.value = Event(it)
                }
                t2?.let {
                    Timber.e(it)
                    val responseBody = it.getResponseBody()
                    if (it.isHttpStatusCode(404) && responseBody == "Merchant not registered") {
                        _createZenithMerchant.value = Event(true)
                    } else {
                        _createZenithMerchant.value = Event(false)
                        message.value = Event(responseBody)
                    }
                }
            }.disposeWith(disposable)
    }

    val cityLoading = MutableLiveData(false)
    fun getCities(state: String) {
        zenithQRService.getCity(state)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                cityLoading.value = true
            }.doFinally {
                cityLoading.value = false
            }
            .retry(2)
            .subscribe { t1, t2 ->
                t1?.let {
                    _zenithCityList.value = Event(it.cityList)
                }
                t2?.let {
                    Timber.e(it.localizedMessage)
                    message.value = Event("An error occurred while fetching cities in $state")
                }
            }
            .disposeWith(disposable)
    }

    fun setSelectedCity(position: Int) {
        createZenithMerchantPayload.value = createZenithMerchantPayload.value?.apply {
            zenithCityList.value?.peekContent()?.let {
                this.cityName = it[position].cityName
                this.regionName = it[position].regionName
            }
        }
        Timber.e(createZenithMerchantPayload.value.toString())
    }

    private lateinit var dataSourceFactory: ZenithQrMCCDataSourceFactory

    fun getZenithMCC(zenithMCCDto: ZenithMCCDto) {
        dataSourceFactory = ZenithQrMCCDataSourceFactory(zenithMCCDto, disposable)
        val networkResourceLiveData: LiveData<Event<NetworkResource>> = Transformations.switchMap(
            dataSourceFactory.itemLiveDataSource
        ) {
            it.networkResource
        }

        val emptyResultLiveData: LiveData<Event<Boolean>> = Transformations.switchMap(
            dataSourceFactory.itemLiveDataSource
        ) {
            it.emptyResultLiveData
        }

        val data: LiveData<PagedList<ZenithMerchantCategory>> =
            LivePagedListBuilder(dataSourceFactory, config).build()
        _paginationHelper.postValue(
            PaginationHelper(
                networkResourceLiveData,
                emptyResultLiveData,
                data
            )
        )
    }

    fun textChanged(filter: String) = subject.onNext(filter)

    fun initSearchFilter() {
        subject
            .debounce(1, TimeUnit.SECONDS)
            .filter { it.isEmpty().not() }
            .distinctUntilChanged()
            .subscribe {
                Timber.e(it)
                getZenithMCC(ZenithMCCDto(it))
            }.disposeWith(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }

    fun textChangeComplete() {
        subject.onComplete()
    }

    fun setSelectedMerchantCategory(it: ZenithMerchantCategory) {
        createZenithMerchantPayload.value = createZenithMerchantPayload.value?.apply {
            this.merchantCategoryCode = it.merchantCategoryCode
            this.merchantCategoryDescription = it.merchantCategoryDescription
        }
    }

    fun registerZenithMerchant() {
        Timber.e(createZenithMerchantPayload.value.toString())
        val payload = createZenithMerchantPayload.value!!
        if (payload.bvn.isNullOrEmpty() || payload.bvn!!.length < 11) {
            message.value = Event("Enter a valid bank verification number")
            return
        }
        if (payload.merchantCategoryCode.isNullOrEmpty()) {
            message.value = Event("Select a merchant category")
            return
        }
        if (payload.cityName.isNullOrEmpty()) {
            message.value = Event("Select a LGA")
            return
        }
        zenithQRService.createZenithQRMerchant(payload)
            .subscribeOn(Schedulers.io()).doOnSubscribe {
                message.postValue(Event("Registering, please wait"))
                registrationInProgress.postValue(true)
            }.doFinally { registrationInProgress.postValue(false) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    _zenithQrRegistrationDone.value = Event(true)
                    message.value = Event(it.message)
                }
                t2?.let {
                    Timber.e(it)
                    message.value = Event("Registration failed")
                    message.value = Event(it.getResponseBody())
                }
            }.disposeWith(disposable)
    }

    fun clearSelectedCity() {
        createZenithMerchantPayload.value = createZenithMerchantPayload.value?.apply {
            this.cityName = null
            this.regionName = null
        }
    }
}