package com.woleapp.netpos.contactless.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.woleapp.netpos.contactless.model.LoadingState
import com.woleapp.netpos.contactless.model.NetworkResource
import com.woleapp.netpos.contactless.model.ZenithMCCDto
import com.woleapp.netpos.contactless.model.ZenithMerchantCategory
import com.woleapp.netpos.contactless.util.Event
import com.woleapp.netpos.contactless.util.disposeWith
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class ZenithQrMCCDataSource(
    private val zenithMCCDto: ZenithMCCDto,
    private val disposables: CompositeDisposable
) :
    PageKeyedDataSource<Int, ZenithMerchantCategory>() {
    private val zenithService = StormApiClient.getZenithQRServiceInstance()
    private val _networkResourceLiveData: MutableLiveData<Event<NetworkResource>> =
        MutableLiveData<Event<NetworkResource>>()
    private val _emptyResultLiveData = MutableLiveData<Event<Boolean>>()

    val emptyResultLiveData: LiveData<Event<Boolean>>
        get() = _emptyResultLiveData

    val networkResource: LiveData<Event<NetworkResource>>
        get() = _networkResourceLiveData

    private var loadedAll = false

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, ZenithMerchantCategory>
    ) {
        _networkResourceLiveData.postValue(Event(NetworkResource(LoadingState.LOADING_INITIAL)))
        val page = "1.20"
        Timber.e(page)
        val query = if (zenithMCCDto.filter.isNullOrEmpty())
            zenithService.getMerchantCategoryList(page)
        else
            zenithService.getMerchantCategoryListWithFilter(zenithMCCDto.filter, page)
        query.retry(2).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    if (it.merchantCategoryList.isEmpty()) {
                        _emptyResultLiveData.postValue(Event(true))
                        loadedAll = true
                    }
                    Timber.e("list")
                    Timber.e(it.merchantCategoryList.toString())

                    _networkResourceLiveData.postValue(
                        Event(
                            NetworkResource(LoadingState.LOADING_COMPLETE)
                        )
                    )
                    callback.onResult(it.merchantCategoryList, null, 2)
                }
                t2?.let {
                    _networkResourceLiveData.postValue(
                        Event(
                            NetworkResource(LoadingState.LOADING_FAILED)
                        )
                    )
                }
            }
            .disposeWith(disposables)
    }

    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, ZenithMerchantCategory>
    ) {

    }

    override fun loadAfter(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, ZenithMerchantCategory>
    ) {
        if (loadedAll)
            return
        _networkResourceLiveData.postValue(Event(NetworkResource(LoadingState.LOADING_MORE)))
        val page = "${params.key}.20"
        Timber.e(page)
        val query = if (zenithMCCDto.filter.isNullOrEmpty())
            zenithService.getMerchantCategoryList(page)
        else
            zenithService.getMerchantCategoryListWithFilter(zenithMCCDto.filter, page)
        query.retry(2).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    if (it.merchantCategoryList.isEmpty())
                        loadedAll = true
                    callback.onResult(it.merchantCategoryList, params.key + 1)

                    _networkResourceLiveData.postValue(
                        Event(
                            NetworkResource(LoadingState.LOADING_COMPLETE)
                        )
                    )
                }
                t2?.let {
                    _networkResourceLiveData.postValue(
                        Event(
                            NetworkResource(LoadingState.LOADING_FAILED)
                        )
                    )
                }
            }
            .disposeWith(disposables)
    }
}