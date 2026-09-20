package com.velocity.auto.car

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.WebView
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.velocity.auto.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Netflix/Stremio don't hand us a bare video Surface the way YouTube's
 * resolved stream does - there's a real interactive web app behind them.
 * So instead this mirrors an off-screen [WebView] onto the car's Surface
 * by redrawing it a few times a second, and turns taps/scrolls from the
 * car screen into synthetic touch events on that WebView. It's the same
 * "screen mirroring" idea apps like Screen2Auto are built around.
 *
 * Caveat worth being upfront about: an unattached WebView's rendering can
 * be unreliable on some Android/WebView-provider combinations (it's never
 * been a fully supported configuration). If a given head unit shows a
 * blank surface here, that's why - the phone-side [com.velocity.auto.web.WebAppActivity]
 * remains the reliable fallback for those services.
 */
class WebMirrorCarScreen(
    carContext: CarContext,
    private val url: String
) : Screen(carContext) {

    private var webView: WebView? = null
    private var renderJob: Job? = null

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            setUpWebView(surfaceContainer)
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            stopMirroring()
        }

        override fun onClick(x: Float, y: Float) {
            dispatchTap(x, y)
        }

        override fun onScroll(distanceX: Float, distanceY: Float) {
            webView?.scrollBy(distanceX.toInt(), distanceY.toInt())
        }
    }

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    stopMirroring()
                }
            }
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView(container: SurfaceContainer) {
        val width = container.width
        val height = container.height
        if (width <= 0 || height <= 0) return

        val view = WebView(carContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = DESKTOP_USER_AGENT
            layout(0, 0, width, height)
        }
        view.loadUrl(url)
        webView = view

        renderJob?.cancel()
        renderJob = lifecycle.coroutineScope.launch {
            while (isActive) {
                renderFrame(container)
                delay(FRAME_INTERVAL_MS)
            }
        }
    }

    private fun renderFrame(container: SurfaceContainer) {
        val view = webView ?: return
        val surface = container.surface
        if (surface == null || !surface.isValid) return
        try {
            val canvas = surface.lockCanvas(null) ?: return
            canvas.drawColor(Color.BLACK)
            view.draw(canvas)
            surface.unlockCanvasAndPost(canvas)
        } catch (e: Exception) {
            // The surface can be torn down between the isValid check and the
            // lock/post calls (screen backgrounded, host disconnecting) -
            // skip this tick and try again next frame.
        }
    }

    private fun dispatchTap(x: Float, y: Float) {
        val view = webView ?: return
        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(downTime, downTime + 40, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(down)
        view.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    private fun stopMirroring() {
        renderJob?.cancel()
        renderJob = null
        webView?.destroy()
        webView = null
    }

    override fun onGetTemplate(): Template {
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle(carContext.getString(R.string.cd_back))
                    .setOnClickListener { screenManager.pop() }
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .setBackgroundColor(CarColor.createCustom(BACKGROUND_COLOR, BACKGROUND_COLOR))
            .build()
    }

    companion object {
        private const val FRAME_INTERVAL_MS = 120L
        private val BACKGROUND_COLOR = Color.parseColor("#0D0F12")
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}
