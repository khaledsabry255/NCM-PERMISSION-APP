package io.github.khaledsabry255.ncmpermission.data

/**
 * Search rules copied from the web app so both behave identically:
 * numbers must match exactly (typing 54 must not return 548), while names
 * tolerate the several written forms of the same Arabic letter.
 */
object Search {

    /** Strips characters that would break a PostgREST or() filter. */
    fun sanitize(text: String): String =
        text.replace(Regex("""[,()"']"""), " ").replace(Regex("""\s+"""), " ").trim()

    /** Arabic-Indic digits map onto ASCII so ٥٤٨ finds 548. */
    fun digitsOnly(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            when (ch) {
                in '٠'..'٩' -> sb.append(ch - '٠')
                in '۰'..'۹' -> sb.append(ch - '۰')
                in '0'..'9' -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun isNumeric(text: String): Boolean {
        val s = sanitize(text)
        return s.isNotEmpty() && s.all {
            it.isDigit() || it in '٠'..'٩' || it in '۰'..'۹' ||
                it == ' ' || it == '-'
        }
    }

    /**
     * Builds a PostgREST ilike pattern where every letter that has more than
     * one written form becomes "_" (matches exactly one character), making
     * ا/أ/إ/آ, ي/ى and ه/ة interchangeable at query time. The stored column is
     * never modified.
     */
    fun namePattern(term: String): String? {
        var cleaned = sanitize(term)
            .replace(Regex("[ً-ْٰ]"), "")   // harakat
            .replace("ـ", "")                          // tatweel
            .replace(Regex("[%_*]"), " ")                   // user-typed wildcards
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (cleaned.isEmpty()) return null

        cleaned = cleaned
            .replace(Regex("[آأإا]"), "_")   // آ أ إ ا
            .replace(Regex("[يى]"), "_")               // ي ى
            .replace(Regex("[هة]"), "_")               // ه ة
            .replace(" ", "*")
        return "*$cleaned*"
    }
}
