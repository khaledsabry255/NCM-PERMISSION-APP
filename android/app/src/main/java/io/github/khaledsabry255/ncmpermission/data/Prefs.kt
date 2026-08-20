package io.github.khaledsabry255.ncmpermission.data

import android.content.Context

/** The phone-side equivalent of the web app's localStorage. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("ncm", Context.MODE_PRIVATE)

    /** The decrypted API key, kept only after a correct PIN. */
    var apiKey: String?
        get() = sp.getString(KEY_API, null)
        set(value) = sp.edit().apply {
            if (value == null) remove(KEY_API) else putString(KEY_API, value)
        }.apply()

    /** "ar" or "en"; Arabic is the default. */
    var lang: String
        get() = sp.getString(KEY_LANG, "ar") ?: "ar"
        set(value) = sp.edit().putString(KEY_LANG, value).apply()

    private companion object {
        const val KEY_API = "ncm_key"
        const val KEY_LANG = "ncm_lang"
    }
}
