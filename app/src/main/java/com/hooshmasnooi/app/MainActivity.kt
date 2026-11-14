package com.hooshmasnooi.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : ComponentActivity() {
    private var atlasWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(5, 15, 35)
        window.navigationBarColor = android.graphics.Color.rgb(5, 15, 35)

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.javaScriptCanOpenWindowsAutomatically = false
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            isFocusable = true
            isFocusableInTouchMode = true
        }

        atlasWebView = webView
        setContentView(webView)

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(PREVIEW_URL)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val view = atlasWebView
                if (view != null && view.canGoBack()) {
                    view.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        atlasWebView?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        atlasWebView?.apply {
            stopLoading()
            loadUrl("about:blank")
            removeAllViews()
            destroy()
        }
        atlasWebView = null
        super.onDestroy()
    }

    private companion object {
        const val PREVIEW_URL = "https://roohalla41-blip.github.io/hesab-sarraf-test/"
    }
}
