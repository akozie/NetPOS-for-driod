package com.woleapp.netpos.contactless.viewmodels

import androidx.lifecycle.ViewModel
import com.woleapp.netpos.contactless.network.NipService
import com.woleapp.netpos.contactless.util.disposeWith
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
            .subscribe { _, _ -> }.disposeWith(disposable)
    }
}