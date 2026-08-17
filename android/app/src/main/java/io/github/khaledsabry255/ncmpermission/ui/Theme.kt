package io.github.khaledsabry255.ncmpermission.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Same palette as the web app: a calm dark surface with gold used only as accent. */
object Ink {
    val Bg = Color(0xFF0B0A08)
    val Panel = Color(0x0BFFFFFF)
    val Panel2 = Color(0x07FFFFFF)
    val Line = Color(0x12FFFFFF)
    val Line2 = Color(0x1FFFFFFF)

    val Gold = Color(0xFFDDA63F)
    val GoldSoft = Color(0xFFF0D69C)
    val GoldTint = Color(0x17DDA63F)

    val Success = Color(0xFF46B177)
    val Warning = Color(0xFFDFA235)
    val Danger = Color(0xFFDD5A4E)
    val Neutral = Color(0xFF8B8471)

    val Text = Color(0xFFF5F1E8)
    val Text2 = Color(0xFFBCB09A)
    val Muted = Color(0xFF877C68)
    val White = Color(0xFFFFFFFF)

    val BrandGreen = Color(0xFF4CB944)
    val NoShot = Color(0xFF3A3A3A)
}

@Composable
fun NcmTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION") isSystemInDarkTheme()   // the app is dark either way
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ink.Gold,
            background = Ink.Bg,
            surface = Ink.Bg,
            onPrimary = Color(0xFF1A1305),
            onBackground = Ink.Text,
            onSurface = Ink.Text
        ),
        content = content
    )
}
