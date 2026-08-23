package io.github.khaledsabry255.ncmpermission.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LockScreen(
    s: Strings,
    checking: Boolean,
    error: String?,
    onToggleLang: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }

    val submit: (String) -> Unit = { entered ->
        keyboard?.hide()
        onSubmit(entered)
    }

    // A refused PIN empties the field, the way the page does. Without this the
    // four digits would still be sitting there when the field is re-enabled,
    // and the field would submit them again, and again.
    LaunchedEffect(error) { if (error != null) pin = "" }

    Box(
        Modifier.fillMaxSize().background(Ink.PageWash).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.widthIn(max = 328.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(9.dp, RoundedCornerShape(20.dp), spotColor = Ink.Gold, ambientColor = Ink.Gold)
                .clip(RoundedCornerShape(20.dp))
                .background(Ink.Panel)
                .border(1.dp, Ink.Line, RoundedCornerShape(20.dp))
                .padding(start = 28.dp, end = 28.dp, top = 38.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Wordmark(big = false)
            Spacer(Modifier.height(26.dp))
            Text(s.pinLabel, fontSize = 12.5.sp, color = Ink.Muted)
            Spacer(Modifier.height(15.dp))

            // Four dots mirror the web app's PIN indicator.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) { i ->
                    Box(
                        Modifier.size(9.dp).clip(CircleShape)
                            .background(if (i < pin.length) Ink.Gold else Ink.Line2)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            BasicPinField(
                value = pin,
                enabled = !checking,
                focusRequester = focus,
                onChange = { pin = it.filter(Char::isDigit).take(8) },
                onDone = { if (pin.isNotEmpty()) submit(pin) }
            )
            // No confirm button: four digits are the whole PIN, so the field
            // below carries the only two things left to say.
            Spacer(Modifier.height(16.dp))
            when {
                checking -> Text(
                    s.checking, color = Ink.Muted,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
                error != null -> Text(
                    error, color = Ink.Danger,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
                else -> Spacer(Modifier.height(20.dp))
            }
        }

        // The page keeps this in the card's own corner, physically left in both
        // languages, so it never jumps sides when the text flips.
        Box(
            Modifier
                .align(AbsoluteAlignment.TopLeft)
                .padding(13.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Ink.GoldTint)
                .border(1.dp, Ink.Line2, RoundedCornerShape(999.dp))
                .clickable { onToggleLang() }
                .padding(horizontal = 11.dp, vertical = 6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlobeIcon(14.dp, Ink.Gold)
                Text(
                    s.otherLang,
                    color = Ink.Gold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Fonts.Condensed,
                    letterSpacing = 1.4.sp
                )
            }
        }
        }
    }
}

@Composable
private fun BasicPinField(
    value: String,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onChange: (String) -> Unit,
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation('•'),
        placeholder = {
            Text(
                "••••",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 19.sp,
                fontFamily = Fonts.Mono,
                letterSpacing = 8.sp,
                color = Ink.Line2,
                textAlign = TextAlign.Center
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(
            fontSize = 19.sp,
            fontFamily = Fonts.Mono,
            textAlign = TextAlign.Center,
            letterSpacing = 8.sp,
            color = Ink.Text
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink.Gold,
            unfocusedBorderColor = Ink.Line2,
            cursorColor = Ink.Gold,
            focusedContainerColor = Ink.Panel2,
            unfocusedContainerColor = Ink.Panel2
        ),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
    )
    // A 4-digit PIN submits itself, so the guard on the gate taps once, not twice.
    LaunchedEffect(value, enabled) { if (enabled && value.length >= 4) onDone() }
}
