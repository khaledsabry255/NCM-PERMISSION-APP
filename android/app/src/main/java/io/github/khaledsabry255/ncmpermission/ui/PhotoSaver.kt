package io.github.khaledsabry255.ncmpermission.ui

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Writes both card photos into the phone's Downloads folder. */
object PhotoSaver {

    /** Returns how many of the two photos were actually saved. */
    suspend fun saveBoth(context: Context, code: String, urls: List<Pair<String, String>>): Int =
        withContext(Dispatchers.IO) {
            var saved = 0
            for ((url, name) in urls) {
                try {
                    if (save(context, url, name)) saved++
                } catch (e: Exception) {
                    // A missing second photo is normal; keep going.
                }
            }
            saved
        }

    private fun save(context: Context, url: String, name: String): Boolean {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
        }
        try {
            if (conn.responseCode !in 200..299) return false
            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                val dir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                File(dir, name).writeBytes(bytes)
            }
            return true
        } finally {
            conn.disconnect()
        }
    }
}
