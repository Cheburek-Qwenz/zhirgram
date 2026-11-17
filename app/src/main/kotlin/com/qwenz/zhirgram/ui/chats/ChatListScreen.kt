package com.qwenz.zhirgram.ui.chats

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.qwenz.zhirgram.data.UiStyle
import com.qwenz.zhirgram.td.ChatItem
import com.qwenz.zhirgram.ui.components.ChatAvatar
import com.qwenz.zhirgram.ui.components.pressCorner
import com.qwenz.zhirgram.ui.components.springPress
import com.qwenz.zhirgram.ui.theme.LocalUiStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatListScreen(
    chats: List<ChatItem>,
    contentPadding: PaddingValues,
    onOpenChat: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chats.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (LocalUiStyle.current == UiStyle.EXPRESSIVE) LoadingIndicator()
            else CircularProgressIndicator()
        }
        return
    }

    val motion = MaterialTheme.motionScheme
    val dir = LocalLayoutDirection.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(dir) + 12.dp,
            end = contentPadding.calculateEndPadding(dir) + 12.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(chats, key = { it.id }) { chat ->
            ChatRow(
                chat = chat,
                onClick = { onOpenChat(chat.id) },
                // Перемещение строки при новом сообщении идёт на пространственной пружине,
                // появление и исчезновение на пружине эффектов (без отскока).
                modifier = Modifier.animateItem(
                    fadeInSpec = motion.defaultEffectsSpec(),
                    placementSpec = motion.defaultSpatialSpec<IntOffset>(),
                    fadeOutSpec = motion.defaultEffectsSpec(),
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatRow(chat: ChatItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    // EXPRESSIVE: угол 24dp, при нажатии сжимается до 12dp. MATERIAL3: всегда 12dp.
    val corner = pressCorner(source, rest = 24.dp, pressed = 12.dp)

    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth().springPress(source),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { ChatAvatar(id = chat.id, title = chat.title) },
            headlineContent = {
                Text(chat.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(chat.preview, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            trailingContent = {
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatTime(chat.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (chat.unread > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(if (chat.unread > 99) "99+" else chat.unread.toString())
                        }
                    }
                }
            },
        )
    }
}

private fun formatTime(unixSeconds: Int): String {
    if (unixSeconds == 0) return ""
    val d = Date(unixSeconds * 1000L)
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = d }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    return SimpleDateFormat(if (sameDay) "HH:mm" else "dd.MM", Locale.getDefault()).format(d)
}
