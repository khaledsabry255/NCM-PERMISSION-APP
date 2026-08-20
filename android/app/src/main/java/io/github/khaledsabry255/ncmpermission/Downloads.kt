package io.github.khaledsabry255.ncmpermission

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast
import java.io.File
import java.util.Collections
import java.util.UUID

/**
 * The save button on a record pulls each photo into a blob and hands it to an
 * anchor, which is how a browser saves a file from another origin. A WebView
 * has no downloads folder of its own, so the blob is read back out through a
 * small bridge and written to the phone's Downloads.
 */
class Downloads(private val context: Context, private val web: WebView) : DownloadListener {

    // A blob can only be read by the page that holds it, so the request has to
    // go back through JavaScript. Each round trip carries a one-shot ticket, so
    // nothing but a download this class started can reach the bridge.
    private val tickets = Collections.synchronizedSet(mutableSetOf<String>())
    private val main = Handler(Looper.getMainLooper())

    init {
        web.addJavascriptInterface(Bridge(), "NCMSave")
    }

    override fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        val name = clean(URLUtil.guessFileName(url, contentDisposition, mimeType))
        if (url.startsWith("blob:")) readBlob(url, name) else viaDownloadManager(url, name)
    }

    private fun readBlob(url: String, name: String) {
        val ticket = UUID.randomUUID().toString()
        tickets.add(ticket)
        web.evaluateJavascript(
            """
            (function(){
              try{
                var x = new XMLHttpRequest();
                x.open('GET', '$url', true);
                x.responseType = 'blob';
                x.onload = function(){
                  var r = new FileReader();
                  r.onloadend = function(){ NCMSave.save('$ticket', String(r.result), '$name'); };
                  r.onerror = function(){ NCMSave.fail('$ticket'); };
                  r.readAsDataURL(x.response);
                };
                x.onerror = function(){ NCMSave.fail('$ticket'); };
                x.send();
              }catch(e){ NCMSave.fail('$ticket'); }
            })();
            """.trimIndent(),
            null
        )
    }

    private fun viaDownloadManager(url: String, name: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        } catch (e: Exception) {
            failed()
        }
    }

    private inner class Bridge {

        /** [dataUrl] arrives as `data:image/jpeg;base64,...` straight from the blob. */
        @JavascriptInterface
        fun save(ticket: String, dataUrl: String, name: String) {
            if (!tickets.remove(ticket)) return
            val comma = dataUrl.indexOf(',')
            if (comma < 0) { failed(); return }
            try {
                val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
                if (bytes.isEmpty() || !write(bytes, name)) failed()
            } catch (e: Exception) {
                failed()
            }
        }

        @JavascriptInterface
        fun fail(ticket: String) {
            if (tickets.remove(ticket)) failed()
        }
    }

    private fun write(bytes: ByteArray, name: String): Boolean {
        val mime = if (name.endsWith(".png", true)) "image/png" else "image/jpeg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return true
        }
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists() && !dir.mkdirs()) return false
        File(dir, name).writeBytes(bytes)
        return true
    }

    /** The page shows its own progress, so only a failure needs saying out loud. */
    private fun failed() {
        main.post {
            Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT)
                .show()
        }
    }

    /** A download name reaches the filesystem, so it keeps nothing but its own. */
    private fun clean(name: String): String {
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.', '_')
        return if (safe.isEmpty()) "ncm-photo.jpg" else safe
    }
}
