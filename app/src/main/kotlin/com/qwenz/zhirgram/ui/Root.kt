package com.qwenz.zhirgram.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qwenz.zhirgram.data.AppSettings
import com.qwenz.zhirgram.data.SettingsRepository
import com.qwenz.zhirgram.data.UiStyle
import com.qwenz.zhirgram.td.AuthState
import com.qwenz.zhirgram.td.TdRepository
import com.qwenz.zhirgram.ui.auth.AuthScreen
import com.qwenz.zhirgram.ui.chats.ChatListScreen
import com.qwenz.zhirgram.ui.settings.SettingsScreen
import com.qwenz.zhirgram.ui.theme.LocalUiStyle
import com.qwenz.zhirgram.ui.theme.ZhirTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZhirRoot(td: TdRepository, settingsRepo: SettingsRepository) {
    val auth by td.auth.collectAsStateWithLifecycle()
    val chats by td.chats.collectAsStateWithLifecycle()
    val settings by settingsRepo.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { td.errors.collect { snackbar.showSnackbar(it) } }

    ZhirTheme(settings) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxSize()) {
                val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                AnimatedContent(
                    targetState = auth is AuthState.Ready,
                    transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
                    label = "root",
                ) { ready ->
                    if (ready) {
                        MainShell(
                            chats = chats,
                            settings = settings,
                            onStyle = { scope.launch { settingsRepo.setStyle(it) } },
                            onDynamicColor = { scope.launch { settingsRepo.setDynamicColor(it) } },
                            onLogOut = td::logOut,
                        )
                    } else {
                        AuthScreen(
                            state = auth,
                            onPhone = td::sendPhone,
                            onCode = td::sendCode,
                            onPassword = td::sendPassword,
                        )
                    }
                }
                SnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding(),
                )
            }
        }
    }
}

private data class Tab(val label: String, val selected: ImageVector, val unselected: ImageVector)

private val tabs = listOf(
    Tab("Чаты", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    Tab("Настройки", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainShell(
    chats: List<com.qwenz.zhirgram.td.ChatItem>,
    settings: AppSettings,
    onStyle: (UiStyle) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onLogOut: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val motion = MaterialTheme.motionScheme
    val spatial = motion.defaultSpatialSpec<IntOffset>()
    val effects = motion.defaultEffectsSpec<Float>()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text(tabs[tab].label) }, scrollBehavior = scroll)
        },
        bottomBar = { BottomNav(selected = tab, onSelect = { tab = it }) },
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(spatial) { dir * it / 5 } + fadeIn(effects)) togetherWith
                    (slideOutHorizontally(spatial) { -dir * it / 5 } + fadeOut(effects))
            },
            label = "tab",
        ) { t ->
            when (t) {
                0 -> ChatListScreen(
                    chats = chats,
                    contentPadding = padding,
                    onOpenChat = { /* TODO: экран чата, см. AGENTS.md этап 3 */ },
                )
                else -> SettingsScreen(
                    settings = settings,
                    contentPadding = padding,
                    onStyle = onStyle,
                    onDynamicColor = onDynamicColor,
                    onLogOut = onLogOut,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    // Подписи оставляем: исследования Google показали, что с ними навигация понятнее.
    if (LocalUiStyle.current == UiStyle.EXPRESSIVE) {
        ShortNavigationBar {
            tabs.forEachIndexed { i, t ->
                ShortNavigationBarItem(
                    selected = selected == i,
                    onClick = { onSelect(i) },
                    icon = {
                        Icon(if (selected == i) t.selected else t.unselected, contentDescription = null)
                    },
                    label = { Text(t.label) },
                )
            }
        }
    } else {
        NavigationBar {
            tabs.forEachIndexed { i, t ->
                NavigationBarItem(
                    selected = selected == i,
                    onClick = { onSelect(i) },
                    icon = {
                        Icon(if (selected == i) t.selected else t.unselected, contentDescription = null)
                    },
                    label = { Text(t.label) },
                )
            }
        }
    }
}
