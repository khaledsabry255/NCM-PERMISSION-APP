package io.github.khaledsabry255.ncmpermission

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** The site the app is a window onto. Trailing slash matters: it is the scope. */
const val SITE = "https://khaledsabry255.github.io/NCM-PERMISSION-APP/"
private const val HOST = "khaledsabry255.github.io"

/**
 * The app is a full-screen window onto the web app rather than a second
 * implementation of it. Rebuilding the same screens in Compose meant every
 * change to the site had to be made twice and the two still drifted apart, so
 * the interface now lives in one place only.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Drawing behind the bars first, then padding the page back inside them,
        // is the only arrangement that behaves the same on every Android version.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        web = WebView(this).apply {
            setBackgroundColor(PAGE_BG)
            overScrollMode = WebView.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(PAGE_BG)
            addView(
                web,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        setContentView(root)

        // Keeps the page clear of the notch, the status bar and the gesture bar,
        // and lifts it above the keyboard when the search box has focus.
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard))
            WindowInsetsCompat.CONSUMED
        }

        configure(web.settings)
        CookieManager.getInstance().setAcceptCookie(true)

        web.webChromeClient = WebChromeClient()
        web.webViewClient = Client()
        web.setDownloadListener(Downloads(this, web))

        // The PIN, the saved key and the language all live in localStorage, so a
        // restored WebView must not start over on the lock screen.
        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl(SITE)

        onBackPressedDispatcher.addCallback(this) {
            if (web.canGoBack()) web.goBack() else finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure(s: WebSettings) {
        s.javaScriptEnabled = true
        // localStorage holds the unlock flag, the decrypted key and the language.
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.setSupportZoom(false)
        s.builtInZoomControls = false
        s.displayZoomControls = false
        // Pinned so the page keeps the proportions it was designed with instead
        // of following the phone's font-size setting.
        s.textZoom = 100
        s.mediaPlaybackRequiresUserGesture = true
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        // The site's own service worker decides what is served from cache, so
        // the WebView is left on its normal behaviour.
        s.cacheMode = WebSettings.LOAD_DEFAULT
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    private inner class Client : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            val url = request.url
            // Anything that is not the site itself belongs to the phone: a phone
            // number to the dialler, anything else to the browser.
            if (url.host.equals(HOST, ignoreCase = true)) return false
            return try {
                startActivity(Intent(Intent.ACTION_VIEW, url))
                true
            } catch (e: ActivityNotFoundException) {
                true
            }
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            // Only a failed page counts; a missing photo must not blank the app.
            if (!request.isForMainFrame) return
            view.loadDataWithBaseURL(SITE, OFFLINE_PAGE, "text/html", "UTF-8", null)
        }
    }

    private companion object {
        val PAGE_BG = Color.parseColor("#EDF2F8")

        /** Shown only when the very first load has no connection to fall back on. */
        val OFFLINE_PAGE = """
            <!DOCTYPE html><html lang="ar" dir="rtl"><head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <style>
              body{margin:0;min-height:100vh;display:flex;align-items:center;
                   justify-content:center;background:#edf2f8;color:#0b1114;
                   font-family:system-ui,sans-serif;text-align:center;padding:24px;}
              h1{font-size:17px;margin:0 0 10px;}
              p{font-size:14px;line-height:1.9;color:#474d54;margin:0 0 20px;}
              a{display:inline-block;background:#e6eef8;color:#1565c0;text-decoration:none;
                font-weight:700;font-size:14px;padding:12px 26px;border-radius:14px;
                border:1px solid #c0d3ec;}
            </style></head><body><div>
              <h1>تعذّر فتح التطبيق</h1>
              <p>مفيش اتصال بالإنترنت.<br>افتح النت وجرّب تاني.</p>
              <a href="$SITE">إعادة المحاولة</a>
            </div></body></html>
        """.trimIndent()
    }
}
