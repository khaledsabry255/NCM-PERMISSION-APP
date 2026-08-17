package io.github.khaledsabry255.ncmpermission.data

/**
 * Same backend contract as the web app. The API key is not here: it is sealed
 * inside [Vault] with the PIN, so a wrong PIN leaves the app unable to reach
 * the database at all rather than merely hiding rows.
 */
object Config {
    const val SUPABASE_URL = "https://ljcalgumlodehoulbwtp.supabase.co"
    const val BUCKET = "employee-photos"
    const val EXPIRY_WINDOW = 30      // days that still count as "expiring soon"
    const val MAX_RESULTS = 30

    fun photoUrl(code: String, second: Boolean): String =
        "$SUPABASE_URL/storage/v1/object/public/$BUCKET/$code${if (second) "_2" else ""}.jpg"
}
