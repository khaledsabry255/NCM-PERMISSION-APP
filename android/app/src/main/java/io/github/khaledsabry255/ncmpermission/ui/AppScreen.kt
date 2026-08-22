package io.github.khaledsabry255.ncmpermission.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.khaledsabry255.ncmpermission.data.Employee
import io.github.khaledsabry255.ncmpermission.data.Repository
import io.github.khaledsabry255.ncmpermission.data.Stats
import kotlinx.coroutines.delay

enum class Tab { SEARCH, BANNED, EXPIRING, EXPIRED }

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AppScreen(repo: Repository, s: Strings, onToggleLang: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.SEARCH) }
    var query by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var stats by remember { mutableStateOf<Stats?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }

    val listState = rememberLazyListState()

    // Keying on the query makes Compose cancel the previous load, which gives
    // both the debounce and the guarantee that a slow reply cannot overwrite a
    // newer one.
    LaunchedEffect(tab, query, reload) {
        failed = false
        try {
            when (tab) {
                Tab.SEARCH -> {
                    if (query.isBlank()) {
                        rows = emptyList()
                        if (stats == null) {
                            loading = true
                            stats = repo.stats()
                        }
                    } else {
                        // Long enough to finish typing a code: 5 may match one
                        // record while the guard is still heading for 548.
                        delay(650)
                        loading = true
                        rows = repo.search(query)
                    }
                }
                Tab.BANNED -> { loading = true; rows = repo.banned() }
                Tab.EXPIRING -> { loading = true; rows = repo.expiring() }
                Tab.EXPIRED -> { loading = true; rows = repo.expired() }
            }
        } catch (e: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }

    // The mark shrinks rather than scrolling away, so the result rises into
    // view while the app still says what it is.
    val compact = tab != Tab.SEARCH || query.isNotBlank()

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(Ink.PageWash)) {
        if (!compact) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp), Alignment.Center) {
                    Wordmark(big = true)
                }
            }
        }

        // Pinned: the search box and both controls stay in reach while results
        // scroll underneath.
        stickyHeader {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Ink.Bg)
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 4.dp)
            ) {
                if (compact) MiniWordmark()
                Toolbar(s, onToggleLang, loading) { stats = null; reload++ }
                Tabs(tab, s) { tab = it }
                if (tab == Tab.SEARCH) SearchBox(query, s) { query = it }
            }
        }

        body(tab, s, query, rows, stats, loading, failed) { reload++ }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

/**
 * Forced to LTR so the language and refresh buttons keep the same physical
 * corners in both languages — they must not swap sides when the text flips.
 */
@Composable
private fun Toolbar(s: Strings, onToggleLang: () -> Unit, busy: Boolean, onRefresh: () -> Unit) {
    // Declared unconditionally, so composition sees the same calls whether or
    // not a refresh is running; only the angle read from it is conditional.
    val turning = rememberInfiniteTransition(label = "refresh")
    val turn by turning.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing)),
        label = "refresh-angle"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Ink.GoldTint)
                    .border(1.dp, Ink.Line2, RoundedCornerShape(999.dp))
                    .clickable { onToggleLang() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
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
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Ink.GoldTint)
                    .border(1.dp, Ink.Line2, CircleShape)
                    .clickable(enabled = !busy) { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.rotate(if (busy) turn else 0f)) {
                    RefreshIcon(17.dp, Ink.Gold)
                }
            }
        }
    }
}

@Composable
private fun Tabs(current: Tab, s: Strings, onPick: (Tab) -> Unit) {
    val labels = listOf(
        Tab.SEARCH to s.tabSearch,
        Tab.BANNED to s.tabBanned,
        Tab.EXPIRING to s.tabExpiring,
        Tab.EXPIRED to s.tabExpired
    )
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEach { (tab, label) ->
            val on = tab == current
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) Ink.Gold else Ink.Panel2)
                    .border(1.dp, if (on) Color.Transparent else Ink.Line, RoundedCornerShape(14.dp))
                    .clickable { onPick(tab) }
                    .padding(vertical = 11.dp, horizontal = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 12.5.sp,
                    fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = if (on) Color.White else Ink.Text2
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SearchBox(query: String, s: Strings, onChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            // Dropping the keyboard is what puts the record on screen.
            keyboard?.hide()
            focus.clearFocus()
        }),
        leadingIcon = {
            // The page's magnifier doubles as the search button.
            Box(
                Modifier
                    .clickable { keyboard?.hide(); focus.clearFocus() }
                    .padding(start = 15.dp, end = 3.dp)
            ) {
                SearchIcon(19.dp, Ink.Muted)
            }
        },
        placeholder = {
            Text(
                s.searchHint,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 15.sp,
                color = Ink.Muted,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Box(Modifier.padding(end = 12.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(25.dp)
                            .clip(CircleShape)
                            .background(Ink.NeutralBg)
                            .clickable { onChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        CloseIcon(13.dp, Ink.Muted)
                    }
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Ink.Panel2,
            unfocusedContainerColor = Ink.Panel2,
            focusedBorderColor = Ink.Gold,
            unfocusedBorderColor = Ink.Line2,
            cursorColor = Ink.Gold,
            focusedTextColor = Ink.Text,
            unfocusedTextColor = Ink.Text
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.body(
    tab: Tab,
    s: Strings,
    query: String,
    rows: List<Employee>,
    stats: Stats?,
    loading: Boolean,
    failed: Boolean,
    onRetry: () -> Unit
) {
    val pad = Modifier.padding(horizontal = 16.dp)
    when {
        failed -> item {
            Column(
                Modifier.fillMaxWidth().then(pad).padding(vertical = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FailIcon(34.dp, Ink.Muted.copy(alpha = 0.5f))
                Spacer(Modifier.height(14.dp))
                Text(s.loadFail, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Ink.Text2)
                Spacer(Modifier.height(7.dp))
                Text(s.loadFailHint, fontSize = 13.sp, color = Ink.Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Ink.Line2),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ink.GoldTint, contentColor = Ink.Gold
                    )
                ) {
                    Text(s.retry, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
                }
            }
        }

        // The page draws waiting cards rather than a spinner, so the shape of
        // what is coming is already on screen when it arrives.
        loading -> item {
            Column(pad) {
                Skeleton()
                if (tab != Tab.SEARCH) Skeleton()
            }
        }

        tab == Tab.SEARCH && query.isBlank() -> item {
            stats?.let { Box(pad) { Tiles(it, s) } }
        }

        rows.isEmpty() -> item {
            val (title, hint) = when (tab) {
                Tab.SEARCH -> s.noMatch to s.noMatchHint
                Tab.BANNED -> s.noBanned to null
                Tab.EXPIRING -> s.noExpiring to null
                Tab.EXPIRED -> s.noExpired to null
            }
            Box(pad) { EmptyState(title, hint) }
        }

        else -> {
            val header = when (tab) {
                Tab.SEARCH -> if (rows.size > 1) s.searchResults to Ink.Gold else null
                Tab.BANNED -> s.bannedList to Ink.Danger
                Tab.EXPIRING -> s.expiringList to Ink.Warning
                Tab.EXPIRED -> s.expiredList to Ink.Danger
            }
            if (header != null) {
                item { Box(pad) { SectionHeader(header.first, rows.size, header.second) } }
            }
            items(rows, key = { it.id }) { emp ->
                Box(pad) { EmployeeCard(emp, s, inBannedTab = tab == Tab.BANNED) }
            }
        }
    }
}

@Composable
private fun Tiles(stats: Stats, s: Strings) {
    Column(
        Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Tile(s.statActive, stats.active, Ink.Text, Ink.Muted, Ink.Panel2, Ink.Line, Modifier.weight(1f))
            Tile(s.statResigned, stats.resigned, Ink.Text2, Ink.Muted, Ink.Panel2, Ink.Line, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Tile(s.statExpiring, stats.expiring, Ink.Warning, Ink.Warning, Ink.WarningBg, Ink.WarningBr, Modifier.weight(1f))
            Tile(s.statBanned, stats.banned, Ink.Danger, Ink.Danger, Ink.DangerBg, Ink.DangerBr, Modifier.weight(1f))
        }
    }
}

/** The four counters, in the page's own swatches rather than a computed wash. */
@Composable
private fun Tile(
    label: String,
    value: Int,
    fg: Color,
    labelColor: Color,
    fill: Color,
    line: Color,
    modifier: Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(fill)
            .border(1.dp, line, RoundedCornerShape(14.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Text(label, fontSize = 11.5.sp, color = labelColor, maxLines = 1)
        Spacer(Modifier.height(7.dp))
        Text(
            "$value",
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = Fonts.Mono,
            color = fg
        )
    }
}
