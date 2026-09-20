package com.velocity.auto.web

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.velocity.auto.databinding.ActivityWebBinding
import com.velocity.auto.util.SystemUiHelper

/**
 * A minimal, car-shaped shell for any streaming web app (Netflix, Stremio,
 * or whatever the user pins from the home screen). This is deliberately
 * "just a browser tab" rather than a real integration - these services
 * don't offer public playback SDKs, so their own web apps are the lightest
 * way in.
 */
class WebAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebBinding
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.goEdgeToEdge(this)
        SystemUiHelper.keepScreenOn(this, true)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        binding.webTitle.text = title

        setUpWebView(binding.webView)
        binding.webView.loadUrl(url)

        binding.backButton.setOnClickListener { handleBack() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleBack()
            }
        )
    }

    private fun handleBack() {
        if (customView != null) {
            binding.webView.webChromeClient?.onHideCustomView()
        } else if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(false)
            builtInZoomControls = false
            // Streaming sites usually gate their full web player behind a
            // desktop check; a mobile UA gets you a "download our app" wall.
            userAgentString = DESKTOP_USER_AGENT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                binding.loadingBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.loadingBar.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme
                if (scheme == "http" || scheme == "https") return false
                // Some sign-in flows hop out to app-link/intent schemes; try
                // to hand those to the system, otherwise just ignore them.
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    true
                } catch (e: ActivityNotFoundException) {
                    true
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                binding.fullscreenContainer.addView(view)
                binding.fullscreenContainer.visibility = View.VISIBLE
                binding.webView.visibility = View.GONE
                binding.topBar.visibility = View.GONE
            }

            override fun onHideCustomView() {
                binding.fullscreenContainer.removeAllViews()
                binding.fullscreenContainer.visibility = View.GONE
                binding.webView.visibility = View.VISIBLE
                binding.topBar.visibility = View.VISIBLE
                customViewCallback?.onCustomViewHidden()
                customView = null
                customViewCallback = null
            }
        }
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_URL = "extra_url"

        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

        fun intentFor(context: Context, title: String, url: String): Intent =
            Intent(context, WebAppActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_URL, url)
    }
}
