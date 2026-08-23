package io.github.khaledsabry255.ncmpermission.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import io.github.khaledsabry255.ncmpermission.data.*
import kotlinx.coroutines.launch

private fun toneBg(tone: Tone) = when (tone) {
    Tone.OK -> Ink.SuccessBg
    Tone.WARN -> Ink.WarningBg
    Tone.BAD -> Ink.DangerBg
    Tone.MUTE -> Ink.NeutralBg
}

private fun toneLine(tone: Tone) = when (tone) {
    Tone.OK -> Ink.SuccessBr
    Tone.WARN -> Ink.WarningBr
    Tone.BAD -> Ink.DangerBr
    Tone.MUTE -> Ink.NeutralBr
}

@Composable
fun EmployeeCard(emp: Employee, s: Strings, inBannedTab: Boolean) {
    // Pinned to RTL: the interface may flip, but a record the guard has learned
    // to read must not rearrange itself around him.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Record(emp, s, inBannedTab)
    }
}

@Composable
private fun Record(emp: Employee, s: Strings, inBannedTab: Boolean) {
    val status = Status.of(emp)
    val tone = when (status.tone) {
        Tone.OK -> Ink.Success
        Tone.WARN -> Ink.Warning
        Tone.BAD -> Ink.Danger
        Tone.MUTE -> Ink.Neutral
    }
    val days = Dates.daysLeft(emp.permitDate)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            // The page lifts the record off the wash with a soft blue shadow.
            .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = Ink.Gold, ambientColor = Ink.Gold)
            .clip(RoundedCornerShape(18.dp))
            .background(Ink.Card)
            .border(1.dp, Ink.CardLine, RoundedCornerShape(18.dp))
    ) {
        Identity(emp, s, status, tone, inBannedTab)
        // The page rules a line under the name and under the permit strip.
        Divider()
        PermitStrip(emp, s, status, tone, days)
        Divider()

        val phoneText = phone(emp.phone)
        val hired = Dates.format(emp.hireDate)
        val leaving = resignEnd(emp, s)

        Column(Modifier.padding(horizontal = 14.dp)) {
            Group(
                s.grpPersonal,
                has = emp.nationalId != null || phoneText != null || emp.address != null
            ) {
                emp.nationalId?.let { FieldRow(s.nationalId, it, mono = true) }
                phoneText?.let { FieldRow(s.phone, it, mono = true) }
                emp.address?.let { FieldRow(s.address, it) }
            }
            Group(
                s.grpWork,
                has = emp.jobTitle != null || emp.department != null || hired != null
            ) {
                emp.jobTitle?.let { FieldRow(s.jobTitle, it) }
                emp.department?.let { FieldRow(s.department, it) }
                hired?.let { FieldRow(s.hireDate, it, mono = true) }
            }
            Group(
                s.grpSecurity,
                has = emp.banReason != null || leaving != null
            ) {
                emp.banReason?.let { FieldRow(s.banReason, it, valueColor = Ink.Danger) }
                leaving?.let { (text, mono) ->
                    FieldRow(s.resignEnd, text, mono = mono, valueColor = Ink.Warning)
                }
            }
        }

        Photos(emp.code, s)
    }
}

/**
 * A labelled block only appears when it actually holds rows, so a man with
 * nothing on his security record never gets a heading with nothing under it.
 */
@Composable
private fun Group(title: String, has: Boolean, content: @Composable ColumnScope.() -> Unit) {
    if (!has) return
    Column {
        GroupTitle(title)
        Column(verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}

@Composable
private fun Identity(
    emp: Employee,
    s: Strings,
    status: PermitStatus,
    tone: Color,
    inBannedTab: Boolean
) {
    Column(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (emp.code.isNotEmpty()) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Ink.GoldTint)
                    .border(1.dp, Ink.Line2, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.code, fontSize = 10.5.sp, color = Ink.Muted)
                Spacer(Modifier.width(7.dp))
                Text(
                    emp.code, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                    color = Ink.Gold, fontFamily = Fonts.Mono
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        val ar = @Composable {
            Text(
                emp.nameAr ?: s.noName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                color = Ink.Text, textAlign = TextAlign.Center, lineHeight = 28.sp
            )
        }
        val en = @Composable {
            emp.nameEn?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it.uppercase(), fontFamily = Fonts.Condensed,
                    fontSize = 19.sp, fontWeight = FontWeight.Bold,
                    color = Ink.Text2, textAlign = TextAlign.Center, letterSpacing = 0.76.sp
                )
            }
        }
        ar(); en()

        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill(
                s.statusLabel(status.code, status.rawLabel),
                tone, toneBg(status.tone), toneLine(status.tone)
            ) { StatusIcon(status.tone, tone) }
            if (emp.resigned) {
                Pill(s.resigned, Ink.Danger, Ink.DangerBg, Ink.DangerBr)
            } else if (inBannedTab) {
                Pill(s.present, Ink.Success, Ink.SuccessBg, Ink.SuccessBr)
            }
            s.categoryLabel(emp)?.let { Pill(it, Ink.Neutral, Ink.NeutralBg, Ink.NeutralBr) }
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.CardLine))
}

@Composable
private fun Pill(
    text: String,
    fg: Color,
    bg: Color,
    br: Color,
    icon: @Composable (() -> Unit)? = null
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, br, RoundedCornerShape(999.dp))
            .padding(horizontal = 13.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) icon()
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = fg)
    }
}

/** The same four marks the page draws beside a permit's word. */
@Composable
private fun StatusIcon(tone: Tone, color: Color) {
    when (tone) {
        Tone.OK -> CheckIcon(13.dp, color)
        Tone.WARN -> WarnIcon(13.dp, color)
        Tone.BAD -> CrossIcon(13.dp, color)
        Tone.MUTE -> RingIcon(13.dp, color)
    }
}

@Composable
private fun PermitStrip(
    emp: Employee,
    s: Strings,
    status: PermitStatus,
    tone: Color,
    days: Int?
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(tone.copy(alpha = 0.16f).compositeOver(Color.White))
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(s.permitStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink.Text)
            Spacer(Modifier.height(3.dp))
            Text(
                s.statusLabel(status.code, status.rawLabel),
                fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = tone
            )
            s.countdown(days)?.let {
                Spacer(Modifier.height(5.dp))
                MixedNumberText(it, 13.sp, Ink.Text, FontWeight.Bold)
            }
            // How much of the last month is left, drawn as the page draws it.
            if (status.tone == Tone.WARN && days != null && days >= 0) {
                Spacer(Modifier.height(9.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Ink.Line)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(
                                (days.toFloat() / Config.EXPIRY_WINDOW).coerceIn(0.04f, 1f)
                            )
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(tone.copy(alpha = 0.85f))
                    )
                }
            }
        }
        val date = Dates.format(emp.permitDate)
        Text(
            date ?: "—",
            fontSize = if (date != null) 24.sp else 17.sp,
            fontWeight = if (date != null) FontWeight.SemiBold else FontWeight.ExtraBold,
            fontFamily = if (date != null) Fonts.Mono else Fonts.Sans,
            color = if (date != null) tone else Ink.Neutral
        )
    }
}

@Composable
private fun Photos(code: String, s: Strings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var label by remember { mutableStateOf(s.download) }
    var busy by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(s) { if (!busy) label = s.download }

    Column(
        Modifier.padding(horizontal = 14.dp).padding(top = 15.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        listOf(false, true).forEach { second ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ink.Shot)
                    .border(1.dp, Ink.ShotLine, RoundedCornerShape(14.dp))
            ) {
                if (code.isEmpty()) {
                    NoPhoto(s)
                } else {
                    SubcomposeAsyncImage(
                        model = Config.photoUrl(code, second),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                        error = { NoPhoto(s) },
                        loading = {
                            Box(Modifier.fillMaxWidth().aspectRatio(1.6f).background(Ink.Shot))
                        }
                    )
                }
            }
        }

        if (code.isNotEmpty()) {
            val fg = when (outcome) {
                true -> Ink.Success
                false -> Ink.Danger
                null -> Ink.Gold
            }
            val fill = when (outcome) {
                true -> Ink.SuccessBg
                false -> Ink.DangerBg
                null -> Ink.Gold
            }
            val edge = when (outcome) {
                true -> Ink.SuccessBr
                false -> Ink.DangerBr
                null -> Ink.Gold
            }
            Button(
                onClick = {
                    if (busy) return@Button
                    busy = true; outcome = null; label = s.downloading
                    scope.launch {
                        val saved = PhotoSaver.saveBoth(
                            context, code,
                            listOf(
                                Config.photoUrl(code, false) to "$code.jpg",
                                Config.photoUrl(code, true) to "${code}_2.jpg"
                            )
                        )
                        outcome = saved > 0
                        label = if (saved > 0) s.downloaded else s.downloadFail
                        busy = false
                    }
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, edge),
                colors = ButtonDefaults.buttonColors(
                    containerColor = fill,
                    contentColor = if (outcome == null) Color.White else fg
                ),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                SaveIcon(16.dp, if (outcome == null) Color.White else fg)
                Spacer(Modifier.width(8.dp))
                Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun NoPhoto(s: Strings) {
    Column(
        // 16:10, the same window the page leaves for a photograph.
        Modifier.fillMaxWidth().aspectRatio(1.6f).background(Ink.NoShot),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PhotoIcon(26.dp, Ink.Neutral.copy(alpha = 0.5f))
        Spacer(Modifier.height(8.dp))
        Text(s.noPhoto, color = Ink.Neutral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun phone(value: String?): String? {
    val digits = value?.let { Search.digitsOnly(it) }.orEmpty()
    if (digits.isEmpty()) return null
    // Stored values lost their leading zero.
    return if (digits.length == 10 && digits[0] != '0') "0$digits" else digits
}

private fun resignEnd(emp: Employee, s: Strings): Pair<String, Boolean>? {
    val formatted = Dates.format(emp.resignationEnd)
    if (formatted != null) return formatted to true
    val text = s.resignEndText(emp.resignationEnd) ?: return null
    return text to false
}
