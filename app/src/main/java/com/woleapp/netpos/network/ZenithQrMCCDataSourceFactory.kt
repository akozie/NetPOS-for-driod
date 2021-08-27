package com.woleapp.netpos.network

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.woleapp.netpos.model.ZenithMCCDto
import com.woleapp.netpos.model.ZenithMerchantCategory
import io.reactivex.disposables.CompositeDisposable

class ZenithQrMCCDataSourceFactory(
    private val zenithMCCDto: ZenithMCCDto,
    private val disposable: CompositeDisposable
) :
    DataSource.Factory<Int, ZenithMerchantCategory>() {
    val itemLiveDataSource = MutableLiveData<ZenithQrMCCDataSource>()

    override fun create(): DataSource<Int, ZenithMerchantCategory> {
        val xx = ZenithQrMCCDataSource(zenithMCCDto, disposable)
        itemLiveDataSource.postValue(xx)
        return xx
    }
}