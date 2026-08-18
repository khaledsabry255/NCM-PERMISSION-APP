package io.github.khaledsabry255.ncmpermission.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Same palette as the web app: a light page with blue used only as accent. */
object Ink {
    val Bg = Color(0xFFEDF2F8)
    val Panel = Color(0xFFFFFFFF)
    val Panel2 = Color(0xFFF3F7FB)
    val Card = Color(0xFFDBE2EA)        // records sit a shade below the page
    val Field = Color(0xFFF7FAFD)
    val Line = Color(0xFFE2E9F2)
    val Line2 = Color(0xFFC0D3EC)
    val CardLine = Color(0xFFC3CDD8)

    // The accent is blue now; the names stay so every call site follows along.
    val Gold = Color(0xFF1565C0)
    val GoldSoft = Color(0xFF1E75D6)
    val GoldTint = Color(0xFFE6EEF8)

    val Success = Color(0xFF12874F);  val SuccessBg = Color(0xFFDFF3E7);  val SuccessBr = Color(0xFF8FD3AE)
    val Warning = Color(0xFFB96E08);  val WarningBg = Color(0xFFFCEBD5);  val WarningBr = Color(0xFFEFC383)
    val Danger  = Color(0xFFC22F2F);  val DangerBg  = Color(0xFFFBE3E3);  val DangerBr  = Color(0xFFEFA9A9)
    val Neutral = Color(0xFF5A646C);  val NeutralBg = Color(0xFFEDF0F3);  val NeutralBr = Color(0xFFCBD3DB)

    val Text = Color(0xFF0B1114)
    val Text2 = Color(0xFF2A3238)
    val Muted = Color(0xFF474D54)
    val White = Color(0xFF0B1114)       // the wordmark reads black on a light page

    val BrandGreen = Color(0xFF4BA548)
    val BrandBlue = Color(0xFF1565C0)
    val BrandNavy = Color(0xFF1F4E79)
    val NoShot = Color(0xFFE6EEF8)
}

@Composable
fun NcmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink.Gold,
            background = Ink.Bg,
            surface = Ink.Bg,
            onPrimary = Color(0xFFFFFFFF),
            onBackground = Ink.Text,
            onSurface = Ink.Text
        ),
        content = content
    )
}
