package com.woleapp.netpos.contactless.di.customDependencies

import android.webkit.WebView
import android.webkit.WebViewClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebViewCallBack @Inject constructor() : WebViewClient() {

    @Deprecated("Deprecated in Java", ReplaceWith("false"))
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = true
}
