package io.github.khaledsabry255.permission

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
// The merged app: one PIN, then a home screen with both systems behind it.
// CHANGE THIS if the app is published under the other repository name — it is
// baked into every APK, and a wrong value is a 404 on every phone.
const val SITE = "https://khaledsabry255.github.io/NCM-PERMISSION/"
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

        // Without this the system refuses to let the app paint beside a camera
        // cut-out and letterboxes that strip in black. Set here rather than left
        // to the theme alone so it does not depend on a resource qualifier
        // resolving the way it is expected to.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        window.statusBarColor = PAGE_BG
        window.navigationBarColor = PAGE_BG

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
        // and lifts it above the keyboard while the search box has focus.
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

        // The unlock flag and the language both live in localStorage, so a
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
        // localStorage holds the unlock flag and the chosen language.
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

    override fun onPause() {
        super.onPause()
        // The page records "this device is unlocked" in localStorage and in a
        // cookie. WebView writes cookies out on its own schedule, so a kill from
        // the task switcher can lose the newest one; flushing here means the PIN
        // is asked once on this phone and not again.
        CookieManager.getInstance().flush()
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
            // Only a failed page counts; a failed row of data must not blank the app.
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
