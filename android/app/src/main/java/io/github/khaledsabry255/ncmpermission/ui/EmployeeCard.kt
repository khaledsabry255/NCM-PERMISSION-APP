package io.github.khaledsabry255.ncmpermission.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@Composable
fun EmployeeCard(emp: Employee, s: Strings, inBannedTab: Boolean) {
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
            .clip(RoundedCornerShape(18.dp))
            .background(Ink.Card)
            .border(1.dp, Ink.CardLine, RoundedCornerShape(18.dp))
    ) {
        Identity(emp, s, status, tone, inBannedTab)
        PermitStrip(emp, s, status, tone, days)

        Column(Modifier.padding(horizontal = 14.dp)) {
            Group(s.grpPersonal) {
                emp.nationalId?.let { FieldRow(s.nationalId, it, mono = true) }
                phone(emp.phone)?.let { FieldRow(s.phone, it, mono = true) }
                emp.address?.let { FieldRow(s.address, it) }
            }
            Group(s.grpWork) {
                emp.jobTitle?.let { FieldRow(s.jobTitle, it) }
                emp.department?.let { FieldRow(s.department, it) }
                Dates.format(emp.hireDate)?.let { FieldRow(s.hireDate, it, mono = true) }
            }
            Group(s.grpSecurity) {
                emp.banReason?.let { FieldRow(s.banReason, it, valueColor = Ink.Danger) }
                resignEnd(emp, s)?.let { (text, mono) ->
                    FieldRow(s.resignEnd, text, mono = mono, valueColor = Ink.Warning)
                }
            }
        }

        Photos(emp.code, s)
    }
}

/** A labelled block only renders when it actually holds rows. */
@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    val body = @Composable { Column(verticalArrangement = Arrangement.spacedBy(7.dp), content = content) }
    Column {
        GroupTitle(title)
        body()
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
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (emp.code.isNotEmpty()) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Ink.GoldTint)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.code, fontSize = 10.5.sp, color = Ink.Muted)
                Spacer(Modifier.width(7.dp))
                Text(
                    emp.code, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                    color = Ink.Gold, fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // In English the Latin name leads; the Arabic name stays underneath.
        val ar = @Composable {
            Text(
                emp.nameAr ?: s.noName, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = Ink.White, textAlign = TextAlign.Center, lineHeight = 28.sp
            )
        }
        val en = @Composable {
            emp.nameEn?.let {
                Text(
                    it.uppercase(), fontSize = 19.sp, fontWeight = FontWeight.Bold,
                    color = Ink.White, textAlign = TextAlign.Center, letterSpacing = 0.8.sp
                )
            }
        }
        if (s.rtl) { ar(); en() } else { en(); ar() }

        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill(s.statusLabel(status.code, status.rawLabel), tone, toneBg(status.tone))
            if (emp.resigned) {
                Pill(s.resigned, Ink.Danger, Ink.DangerBg)
            } else if (inBannedTab) {
                Pill(s.present, Ink.Success, Ink.SuccessBg)
            }
            s.categoryLabel(emp)?.let { Pill(it, Ink.Neutral, Ink.NeutralBg) }
        }
    }
}

@Composable
private fun Pill(text: String, fg: Color, bg: Color) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 13.dp, vertical = 5.dp),
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        color = fg
    )
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
            .background(tone.copy(alpha = 0.14f))
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(s.permitStatus, fontSize = 11.5.sp, color = Ink.Muted)
            Spacer(Modifier.height(3.dp))
            Text(
                s.statusLabel(status.code, status.rawLabel),
                fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = tone
            )
            s.countdown(days)?.let {
                Spacer(Modifier.height(5.dp))
                MixedNumberText(it, 12.5.sp, Ink.Text2, FontWeight.SemiBold)
            }
        }
        val date = Dates.format(emp.permitDate)
        Text(
            date ?: "—",
            fontSize = if (date != null) 24.sp else 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (date != null) FontFamily.Monospace else FontFamily.Default,
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
                    .background(Ink.Field)
                    .border(1.dp, Ink.Line2, RoundedCornerShape(14.dp))
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
                            Box(Modifier.fillMaxWidth().height(150.dp).background(Ink.Field))
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (outcome == null) Ink.Gold else fg.copy(alpha = 0.14f),
                    contentColor = if (outcome == null) Color.White else fg
                ),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NoPhoto(s: Strings) {
    Box(
        Modifier.fillMaxWidth().height(150.dp).background(Ink.NoShot),
        contentAlignment = Alignment.Center
    ) {
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
