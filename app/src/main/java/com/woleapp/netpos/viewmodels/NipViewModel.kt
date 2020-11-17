package com.woleapp.netpos.viewmodels

import androidx.lifecycle.ViewModel
import com.woleapp.netpos.network.NipService
import com.woleapp.netpos.util.disposeWith
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class NipViewModel : ViewModel() {
    private lateinit var nipService: NipService
    private val disposable = CompositeDisposable()

    fun setNipService(nipService: NipService) {
        this.nipService = nipService
    }

    fun getNipByReference(reference: String) {
        nipService.getNotificationByReference(reference)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 -> }.disposeWith(disposable)
    }
}