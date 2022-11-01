package com.woleapp.netpos.contactless.util

import io.reactivex.SingleTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

object RxJavaUtils {
    fun <T> getSingleTransformer(errorTag: String): SingleTransformer<T, T> =
        SingleTransformer {
            it.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { throwable ->
                    Timber.d(errorTag, throwable.localizedMessage)
                }
        }
}
