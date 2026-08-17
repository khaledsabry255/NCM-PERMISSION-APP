package io.github.khaledsabry255.ncmpermission.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
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

@Composable
fun AppScreen(repo: Repository, s: Strings, onToggleLang: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.SEARCH) }
    var query by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var stats by remember { mutableStateOf<Stats?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }

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
                        delay(260)
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

    Column(Modifier.fillMaxSize().background(Ink.Bg)) {
        Header(s, onToggleLang, refreshing = loading) {
            stats = null
            reload++
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Tabs(tab, s) { tab = it }
            if (tab == Tab.SEARCH) {
                SearchBox(query, s) { query = it }
            }
        }
        Body(tab, s, query, rows, stats, loading, failed) { reload++ }
    }
}

/**
 * Forced to LTR so the language and refresh buttons keep the same physical
 * corners in both languages — they must not swap sides when the text flips.
 */
@Composable
private fun Header(s: Strings, onToggleLang: () -> Unit, refreshing: Boolean, onRefresh: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xE0161310))
                        .border(1.dp, Ink.Gold.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                        .clickable { onToggleLang() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(s.otherLang, color = Ink.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xE0161310))
                        .border(1.dp, Ink.Gold.copy(alpha = 0.3f), CircleShape)
                        .clickable(enabled = !refreshing) { onRefresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↻", color = Ink.Gold, fontSize = 17.sp)
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
                Wordmark(big = true)
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
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEach { (tab, label) ->
            val on = tab == current
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) Ink.Gold else Ink.Panel2)
                    .border(
                        1.dp,
                        if (on) Color.Transparent else Ink.Line,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onPick(tab) }
                    .padding(vertical = 11.dp, horizontal = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = if (on) Color(0xFF1A1305) else Ink.Text2
                )
            }
        }
    }
}

@Composable
private fun SearchBox(query: String, s: Strings, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(s.searchHint, fontSize = 14.sp, color = Ink.Muted) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Text(
                    "✕",
                    color = Ink.Muted,
                    modifier = Modifier.clickable { onChange("") }.padding(8.dp)
                )
            }
        },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink.Gold.copy(alpha = 0.45f),
            unfocusedBorderColor = Ink.Line2,
            cursorColor = Ink.Gold,
            focusedTextColor = Ink.Text,
            unfocusedTextColor = Ink.Text
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Body(
    tab: Tab,
    s: Strings,
    query: String,
    rows: List<Employee>,
    stats: Stats?,
    loading: Boolean,
    failed: Boolean,
    onRetry: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        when {
            failed -> item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.loadFail, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink.Text2)
                    Spacer(Modifier.height(7.dp))
                    Text(s.loadFailHint, fontSize = 13.sp, color = Ink.Muted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Ink.GoldTint, contentColor = Ink.Gold
                        )
                    ) { Text(s.retry, fontWeight = FontWeight.Bold) }
                }
            }

            loading -> item {
                Box(Modifier.fillMaxWidth().padding(vertical = 60.dp), Alignment.Center) {
                    CircularProgressIndicator(color = Ink.Gold, strokeWidth = 2.dp)
                }
            }

            tab == Tab.SEARCH && query.isBlank() -> item {
                stats?.let { Tiles(it, s) }
            }

            rows.isEmpty() -> item {
                val (title, hint) = when (tab) {
                    Tab.SEARCH -> s.noMatch to s.noMatchHint
                    Tab.BANNED -> s.noBanned to null
                    Tab.EXPIRING -> s.noExpiring to null
                    Tab.EXPIRED -> s.noExpired to null
                }
                EmptyState(title, hint)
            }

            else -> {
                val header = when (tab) {
                    Tab.SEARCH -> if (rows.size > 1) s.searchResults to Ink.Gold else null
                    Tab.BANNED -> s.bannedList to Ink.Danger
                    Tab.EXPIRING -> s.expiringList to Ink.Warning
                    Tab.EXPIRED -> s.expiredList to Ink.Danger
                }
                if (header != null) {
                    item { SectionHeader(header.first, rows.size, header.second) }
                }
                items(rows, key = { it.id }) { emp ->
                    EmployeeCard(emp, s, inBannedTab = tab == Tab.BANNED)
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun Tiles(stats: Stats, s: Strings) {
    Column(
        Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Tile(s.statActive, stats.active, Ink.Text, null, Modifier.weight(1f))
            Tile(s.statResigned, stats.resigned, Ink.Text2, null, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Tile(s.statExpiring, stats.expiring, Ink.Warning, Ink.Warning, Modifier.weight(1f))
            Tile(s.statBanned, stats.banned, Ink.Danger, Ink.Danger, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Tile(label: String, value: Int, fg: Color, accent: Color?, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent?.copy(alpha = 0.1f) ?: Ink.Panel2)
            .border(
                1.dp,
                accent?.copy(alpha = 0.25f) ?: Ink.Line,
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Text(label, fontSize = 11.5.sp, color = accent ?: Ink.Muted, maxLines = 1)
        Spacer(Modifier.height(7.dp))
        Text(
            "$value",
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = fg
        )
    }
}
