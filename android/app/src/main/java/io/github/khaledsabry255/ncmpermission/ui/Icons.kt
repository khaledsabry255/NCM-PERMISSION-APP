package io.github.khaledsabry255.ncmpermission.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The site's icons, drawn rather than typed. The app used to reach for text
 * glyphs (⌕ ↻ ✕), which land on whatever shape the phone's font happens to
 * carry — thin, differently weighted, sometimes missing. Every icon here is
 * the same 24x24 geometry the page draws in SVG, so the two look alike.
 */

/** Scales a 24-unit design coordinate onto the drawn box. */
private fun DrawScope.u() = size.minDimension / 24f

private fun DrawScope.line(
    x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float
) {
    val u = u()
    drawLine(
        color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u),
        strokeWidth = width * u, cap = StrokeCap.Round
    )
}

private fun DrawScope.ring(cx: Float, cy: Float, r: Float, color: Color, width: Float) {
    val u = u()
    drawCircle(color, r * u, Offset(cx * u, cy * u), style = Stroke(width * u))
}

@Composable
fun SearchIcon(size: Dp, color: Color, stroke: Float = 2f) {
    Canvas(Modifier.size(size)) {
        ring(11f, 11f, 7f, color, stroke)
        line(20f, 20f, 16.4f, 16.4f, color, stroke)
    }
}

@Composable
fun GlobeIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val u = u()
        ring(12f, 12f, 8.6f, color, 1.9f)
        line(3.5f, 9.5f, 20.5f, 9.5f, color, 1.5f)
        line(3.5f, 14.5f, 20.5f, 14.5f, color, 1.5f)
        // the meridian: an upright oval through both poles
        drawOval(
            color,
            topLeft = Offset(8.7f * u, 3.4f * u),
            size = Size(6.6f * u, 17.2f * u),
            style = Stroke(1.5f * u)
        )
    }
}

@Composable
fun RefreshIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val u = u()
        val w = 2.1f * u
        val box = Size(16f * u, 16f * u)
        val at = Offset(4f * u, 4f * u)
        drawArc(color, 30f, 150f, false, at, box, style = Stroke(w, cap = StrokeCap.Round))
        drawArc(color, 210f, 150f, false, at, box, style = Stroke(w, cap = StrokeCap.Round))
        // the two arrow corners the page draws at opposite ends
        line(4f, 4f, 4f, 10f, color, 2.1f)
        line(4f, 10f, 10f, 10f, color, 2.1f)
        line(20f, 20f, 20f, 14f, color, 2.1f)
        line(20f, 14f, 14f, 14f, color, 2.1f)
    }
}

@Composable
fun CloseIcon(size: Dp, color: Color, stroke: Float = 2.4f) {
    Canvas(Modifier.size(size)) {
        line(6f, 6f, 18f, 18f, color, stroke)
        line(18f, 6f, 6f, 18f, color, stroke)
    }
}

/** A tick, for a permit that is in order. */
@Composable
fun CheckIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        line(5f, 12.6f, 9.4f, 17f, color, 2.8f)
        line(9.4f, 17f, 19f, 7.4f, color, 2.8f)
    }
}

/** An exclamation, for a permit that is running out. */
@Composable
fun WarnIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val u = u()
        line(12f, 7.6f, 12f, 13f, color, 2.6f)
        drawCircle(color, 1.4f * u, Offset(12f * u, 17f * u))
    }
}

/** A cross, for banned or expired. */
@Composable
fun CrossIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        line(7f, 7f, 17f, 17f, color, 2.8f)
        line(17f, 7f, 7f, 17f, color, 2.8f)
    }
}

/** A plain ring, for anything the page has no stronger word for. */
@Composable
fun RingIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) { ring(12f, 12f, 7f, color, 2.4f) }
}

@Composable
fun PhotoIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val u = u()
        drawRoundRect(
            color,
            topLeft = Offset(3f * u, 5f * u),
            size = Size(18f * u, 14f * u),
            cornerRadius = CornerRadius(2.4f * u, 2.4f * u),
            style = Stroke(1.8f * u)
        )
        ring(9f, 10.5f, 1.9f, color, 1.8f)
        line(4.5f, 17.5f, 9.2f, 13.3f, color, 1.8f)
        line(9.2f, 13.3f, 12.8f, 16.3f, color, 1.8f)
        line(12.8f, 16.3f, 15.8f, 13.9f, color, 1.8f)
        line(15.8f, 13.9f, 19.5f, 17.1f, color, 1.8f)
    }
}

/** The save control under both photographs. */
@Composable
fun SaveIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        line(12f, 4f, 12f, 14.5f, color, 2.1f)
        line(7.6f, 10.6f, 12f, 15f, color, 2.1f)
        line(12f, 15f, 16.4f, 10.6f, color, 2.1f)
        line(4.5f, 17.5f, 4.5f, 18.7f, color, 2.1f)
        line(4.5f, 18.7f, 19.5f, 18.7f, color, 2.1f)
        line(19.5f, 18.7f, 19.5f, 17.5f, color, 2.1f)
    }
}

/** The empty-handed face the page shows when a search finds nobody. */
@Composable
fun NoResultIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        ring(12f, 12f, 8.4f, color, 1.8f)
        line(9f, 12f, 15f, 12f, color, 1.8f)
    }
}

/** The warning face the page shows when the data cannot be reached. */
@Composable
fun FailIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val u = u()
        ring(12f, 12f, 8.4f, color, 1.8f)
        line(12f, 8f, 12f, 13f, color, 2f)
        drawCircle(color, 1.2f * u, Offset(12f * u, 16.6f * u))
    }
}
