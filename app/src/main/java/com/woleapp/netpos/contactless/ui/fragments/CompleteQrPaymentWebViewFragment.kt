package com.woleapp.netpos.contactless.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.FragmentCompleteQrPaymentWebViewBinding
import com.woleapp.netpos.contactless.di.customDependencies.JavaScriptInterface
import com.woleapp.netpos.contactless.di.customDependencies.WebViewCallBack
import com.woleapp.netpos.contactless.di.customDependencies.assistedFactories.JavaScriptInterfaceAssistedFactory
import com.woleapp.netpos.contactless.util.AppConstants.STRING_TAG_JAVASCRIPT_INTERFACE_TAG
import com.woleapp.netpos.contactless.viewmodels.ScanQrViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CompleteQrPaymentWebViewFragment @Inject constructor() : Fragment() {
    private lateinit var binding: FragmentCompleteQrPaymentWebViewBinding
    private lateinit var wbView: WebView
    private val viewModel by activityViewModels<ScanQrViewModel>()
    private lateinit var webSettings: WebSettings

    @Inject
    lateinit var customWebViewClient: WebViewCallBack

    private lateinit var javaScriptInterface: JavaScriptInterface

    @Inject
    lateinit var javaScriptInterfaceAssistedFactory: JavaScriptInterfaceAssistedFactory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_complete_qr_payment_web_view,
            container,
            false
        )

        // Handle Back Press
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (wbView != null) {
                        if (wbView.canGoBack()) {
                            wbView.goBack()
                        } else {
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                }
            }
        )

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wbView = binding.webView

        viewModel.sendQrToServerResponse.observe(viewLifecycleOwner) { response ->
            response.data?.let {
                javaScriptInterface = javaScriptInterfaceAssistedFactory.createJavaScriptInterface(
                    parentFragmentManager,
                    it.TermUrl,
                    it.MD,
                    it.PaReq,
                    it.ACSUrl,
                    it.transId
                )
            }
            setUpWebView(wbView)
        }
    }

    private fun setUpWebView(webView: WebView) {
        webSettings = webView.settings
        webSettings.apply {
            javaScriptEnabled = true
        }
        webView.apply {
            webViewClient = customWebViewClient
            webChromeClient = WebChromeClient()
            addJavascriptInterface(javaScriptInterface, STRING_TAG_JAVASCRIPT_INTERFACE_TAG)
            loadUrl("file:///android_asset/3ds_pay.html")
        }
    }
}
