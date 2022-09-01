package com.woleapp.netpos.contactless.di.customDependencies

import android.webkit.JavascriptInterface
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.gson.Gson
import com.woleapp.netpos.contactless.BuildConfig
import com.woleapp.netpos.contactless.model.QrTransactionResponseModel
import com.woleapp.netpos.contactless.ui.dialog.ResponseModal
import com.woleapp.netpos.contactless.util.AppConstants.QR_TRANSACTION_RESULT_BUNDLE_KEY
import com.woleapp.netpos.contactless.util.AppConstants.QR_TRANSACTION_RESULT_REQUEST_KEY
import com.woleapp.netpos.contactless.util.AppConstants.STRING_QR_RESPONSE_MODAL_DIALOG_TAG
import com.woleapp.netpos.contactless.util.Singletons
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Inject

class JavaScriptInterface @AssistedInject constructor(
    @Assisted private val fragmentManager: FragmentManager,
    @Assisted("termUrl") private val termUrl: String,
    @Assisted("md") private val md: String,
    @Assisted("paReq") private val paReq: String,
    @Assisted("acsUrl") private val acsUrl: String,
    @Assisted("transId") private val transId: String
) {
    @Inject
    lateinit var responseModal: ResponseModal
    private val webViewBaseUrl =
        BuildConfig.STRING_WEB_VIEW_BASE_URL + BuildConfig.STRING_MERCHANT_ID + "/"

    @JavascriptInterface
    fun sendValueToWebView() =
        "$termUrl<======>$md<======>$paReq<======>$acsUrl<======>$transId<======>$webViewBaseUrl"

    @JavascriptInterface
    fun webViewCallback(webViewResponse: String) {
        val responseFromWebView = Gson().fromJson(
            webViewResponse,
            QrTransactionResponseModel::class.java
        )
        val response = responseFromWebView.mapQrTransRespToQrRespFinalModel(
            accType = "",
            terminalId = Singletons.getCurrentlyLoggedInUser()?.terminal_id ?: "",
            merchantId = BuildConfig.STRING_MERCHANT_ID
        )
        fragmentManager.setFragmentResult(
            QR_TRANSACTION_RESULT_REQUEST_KEY,
            bundleOf(QR_TRANSACTION_RESULT_BUNDLE_KEY to response)
        )
        fragmentManager.popBackStack()
        responseModal.show(fragmentManager, STRING_QR_RESPONSE_MODAL_DIALOG_TAG)
    }
}
