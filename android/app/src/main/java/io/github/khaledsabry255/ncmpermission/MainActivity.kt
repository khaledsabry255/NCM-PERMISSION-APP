package io.github.khaledsabry255.ncmpermission

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import io.github.khaledsabry255.ncmpermission.data.Prefs
import io.github.khaledsabry255.ncmpermission.data.Repository
import io.github.khaledsabry255.ncmpermission.data.Vault
import io.github.khaledsabry255.ncmpermission.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)

        setContent {
            NcmTheme {
                var lang by remember { mutableStateOf(prefs.lang) }
                var apiKey by remember { mutableStateOf(prefs.apiKey) }
                var checking by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()
                val strings = remember(lang) { Strings.of(lang) }

                val toggleLang = {
                    lang = if (lang == "ar") "en" else "ar"
                    prefs.lang = lang
                }

                CompositionLocalProvider(
                    LocalLayoutDirection provides
                        if (strings.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    // safeDrawing keeps every edge clear of the notch, the status
                    // bar and the gesture bar, which targetSdk 35 draws under.
                    Surface(
                        Modifier
                            .fillMaxSize()
                            .background(Ink.Bg)
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                        color = Ink.Bg
                    ) {
                        val key = apiKey
                        if (key == null) {
                            LockScreen(strings, checking, error) { pin ->
                                if (checking) return@LockScreen
                                checking = true
                                error = null
                                scope.launch {
                                    // 300k PBKDF2 rounds: never on the main thread.
                                    val unlocked = withContext(Dispatchers.Default) {
                                        Vault.unlock(pin)
                                    }
                                    if (unlocked != null) {
                                        prefs.apiKey = unlocked
                                        apiKey = unlocked
                                    } else {
                                        error = strings.wrongPin
                                    }
                                    checking = false
                                }
                            }
                        } else {
                            val repo = remember(key) { Repository(key) }
                            AppScreen(repo, strings, toggleLang)
                        }
                    }
                }
            }
        }
    }
}
