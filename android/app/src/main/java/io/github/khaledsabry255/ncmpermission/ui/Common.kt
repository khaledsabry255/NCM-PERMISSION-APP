package io.github.khaledsabry255.ncmpermission.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The NCM wordmark: white letters with the company green on the C. */
@Composable
fun Wordmark(big: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            val size = if (big) 34.sp else 30.sp
            Text("N", fontSize = size, fontWeight = FontWeight.Black, color = Ink.White)
            Text("C", fontSize = size, fontWeight = FontWeight.Black, color = Ink.BrandGreen)
            Text("M", fontSize = size, fontWeight = FontWeight.Black, color = Ink.White)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "NUCLEAR CONCRETE MIXES",
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = Ink.Gold.copy(alpha = 0.92f)
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.width(46.dp).height(1.dp).background(Ink.Gold.copy(alpha = 0.5f)))
        Spacer(Modifier.height(9.dp))
        Text(
            "PERMISSION",
            fontSize = if (big) 20.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            color = Ink.Gold
        )
    }
}

/** A field row: label on the start edge, value on the end edge, in its own panel. */
@Composable
fun FieldRow(label: String, value: String, mono: Boolean = false, valueColor: Color = Ink.Text) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Ink.Panel2)
            .border(1.dp, Ink.Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.5.sp, color = Ink.Muted)
        Spacer(Modifier.width(14.dp))
        Text(
            value,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.End,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 10.5.sp,
        letterSpacing = 1.4.sp,
        color = Ink.Muted,
        modifier = Modifier.padding(start = 4.dp, top = 15.dp, bottom = 8.dp)
    )
}

@Composable
fun SectionHeader(title: String, count: Int, tone: Color) {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.5.sp, color = Ink.Muted)
        Spacer(Modifier.width(10.dp))
        Text(
            "$count",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = tone
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Ink.Line))
    }
}

@Composable
fun EmptyState(title: String, hint: String? = null) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 52.dp, horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink.Text2)
        if (hint != null) {
            Spacer(Modifier.height(7.dp))
            Text(hint, fontSize = 13.sp, color = Ink.Muted, textAlign = TextAlign.Center)
        }
    }
}
