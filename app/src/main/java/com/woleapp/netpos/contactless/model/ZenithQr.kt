package com.woleapp.netpos.contactless.model

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.woleapp.netpos.contactless.util.Event

data class ZenithQr(val qrCode: String)
data class ZenithMerchantCategoryList(val merchantCategoryList: List<ZenithMerchantCategory>)
data class ZenithMerchantCategory(
    val merchantCategoryCode: String,
    val merchantCategoryDescription: String
)

data class CreateZenithMerchantPayload(
    var cityName: String? = null,
    var regionName: String? = null,
    var merchantCategoryCode: String? = null,
    var merchantCategoryDescription: String? = null,
    var bvn: String? = null
)

data class CreateZenithMerchantResponse(val message: String)

data class ZenithCityList(val cityList: List<ZenithCity>)
data class ZenithCity(
    val cityName: String,
    val cityCode: String,
    val regionCode: String,
    val regionName: String
)

data class ZenithMCCDto(val filter: String? = null)

class NetworkResource {
    var loadingState: LoadingState
    var message: String? = null

    constructor(loadingState: LoadingState, message: String?) {
        this.message = message
        this.loadingState = loadingState
    }

    constructor(loadingState: LoadingState) {
        this.loadingState = loadingState
    }
}

class PaginationHelper {
    var eventLiveData: LiveData<Event<NetworkResource>>? = null
    var emptyResultLiveData: LiveData<Event<Boolean>>? = null
    var data: LiveData<PagedList<ZenithMerchantCategory>>? = null

    constructor(
        networkResourceLiveData: LiveData<Event<NetworkResource>>,
        data:LiveData<PagedList<ZenithMerchantCategory>>?
    ) {
        eventLiveData = networkResourceLiveData
        this.data = data
    }

    constructor(
        networkResourceLiveData: LiveData<Event<NetworkResource>>,
        emptyResultLiveData: LiveData<Event<Boolean>>,
        data: LiveData<PagedList<ZenithMerchantCategory>>?
    ) {
        this.eventLiveData = networkResourceLiveData
        this.emptyResultLiveData = emptyResultLiveData
        this.data = data
    }
}

enum class LoadingState {
    LOADING_INITIAL,
    LOADING_MORE,
    LOADING_COMPLETE,
    LOADING_FAILED
}