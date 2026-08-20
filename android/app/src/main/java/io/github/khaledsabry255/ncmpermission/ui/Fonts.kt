package io.github.khaledsabry255.ncmpermission.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.khaledsabry255.ncmpermission.R

/**
 * The same three faces the site is drawn with, bundled rather than fetched, so
 * the app and the page look alike instead of the app falling back to whatever
 * the phone ships with. Weights match the ones the CSS asks for.
 */
object Fonts {

    /** Body and Arabic text. */
    val Sans = FontFamily(
        Font(R.font.tajawal_regular, FontWeight.Normal),
        Font(R.font.tajawal_medium, FontWeight.Medium),
        Font(R.font.tajawal_bold, FontWeight.Bold),
        Font(R.font.tajawal_extrabold, FontWeight.ExtraBold),
        Font(R.font.tajawal_black, FontWeight.Black)
    )

    /** Codes, national IDs, phones, dates — anything that must line up. */
    val Mono = FontFamily(
        Font(R.font.plex_mono_regular, FontWeight.Normal),
        Font(R.font.plex_mono_medium, FontWeight.Medium),
        Font(R.font.plex_mono_semibold, FontWeight.SemiBold)
    )

    /** The Latin name and the PERMISSION wordmark. */
    val Condensed = FontFamily(
        Font(R.font.barlow_condensed_medium, FontWeight.Medium),
        Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.barlow_condensed_bold, FontWeight.Bold)
    )
}
