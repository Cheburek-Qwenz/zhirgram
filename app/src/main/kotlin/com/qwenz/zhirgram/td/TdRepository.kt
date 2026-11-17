package com.qwenz.zhirgram.td

import android.content.Context
import android.os.Build
import com.qwenz.zhirgram.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.Locale

sealed interface AuthState {
    data object Loading : AuthState
    data object WaitPhone : AuthState
    data object WaitCode : AuthState
    data class WaitPassword(val hint: String) : AuthState
    data object Ready : AuthState
    /** Шаг входа, которого ещё нет в UI (email, регистрация, подтверждение с другого устройства). */
    data class Unsupported(val name: String) : AuthState
}

data class ChatItem(
    val id: Long,
    val title: String,
    val preview: String,
    val unread: Int,
    val date: Int,
    val pinned: Boolean,
)

/**
 * Тонкая обёртка над TDLib. Классы org.drinkless.tdlib.* (Client, TdApi) появляются в проекте
 * только после workflow build-tdlib.yml, в git их нет.
 *
 * Обновления TDLib приходят в потоке TDLib, поэтому состояние отдаём через StateFlow.
 */
class TdRepository(private val context: Context) {

    private val _auth = MutableStateFlow<AuthState>(AuthState.Loading)
    val auth: StateFlow<AuthState> = _auth

    private val _chats = MutableStateFlow<List<ChatItem>>(emptyList())
    val chats: StateFlow<List<ChatItem>> = _chats

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errors: SharedFlow<String> = _errors

    private val chatMap = HashMap<Long, TdApi.Chat>()
    private var client: Client? = null
    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        Client.execute(TdApi.SetLogVerbosityLevel(1))
        client = Client.create(
            { obj -> onUpdate(obj) },
            { e -> report(e.message ?: e.javaClass.simpleName) },
            { e -> report(e.message ?: e.javaClass.simpleName) },
        )
    }

    // ---- действия из UI ----

    fun sendPhone(phone: String) = send(TdApi.SetAuthenticationPhoneNumber().apply {
        phoneNumber = phone.trim()
    })

    fun sendCode(code: String) = send(TdApi.CheckAuthenticationCode().apply {
        this.code = code.trim()
    })

    fun sendPassword(pass: String) = send(TdApi.CheckAuthenticationPassword().apply {
        password = pass
    })

    fun logOut() = send(TdApi.LogOut())

    // ---- внутреннее ----

    private fun send(f: TdApi.Function<*>, onOk: (TdApi.Object) -> Unit = {}) {
        client?.send(f) { r ->
            if (r is TdApi.Error) report("${r.code}: ${r.message}") else onOk(r)
        }
    }

    private fun report(msg: String) {
        _errors.tryEmit(msg)
    }

    private fun onUpdate(obj: TdApi.Object) {
        when (obj) {
            is TdApi.UpdateAuthorizationState -> onAuth(obj.authorizationState)

            is TdApi.UpdateNewChat -> {
                synchronized(chatMap) { chatMap[obj.chat.id] = obj.chat }
                rebuild()
            }
            is TdApi.UpdateChatTitle -> mutate(obj.chatId) { it.title = obj.title }
            is TdApi.UpdateChatLastMessage -> mutate(obj.chatId) {
                it.lastMessage = obj.lastMessage
                it.positions = obj.positions
            }
            is TdApi.UpdateChatPosition -> mutate(obj.chatId) { chat ->
                val p = obj.position
                val rest = chat.positions.filter { !sameList(it.list, p.list) }
                chat.positions = (if (p.order == 0L) rest else rest + p).toTypedArray()
            }
            is TdApi.UpdateChatReadInbox -> mutate(obj.chatId) {
                it.lastReadInboxMessageId = obj.lastReadInboxMessageId
                it.unreadCount = obj.unreadCount
            }
            else -> Unit
        }
    }

    private fun onAuth(s: TdApi.AuthorizationState) {
        when (s) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> sendParameters()
            is TdApi.AuthorizationStateWaitPhoneNumber -> _auth.value = AuthState.WaitPhone
            is TdApi.AuthorizationStateWaitCode -> _auth.value = AuthState.WaitCode
            is TdApi.AuthorizationStateWaitPassword ->
                _auth.value = AuthState.WaitPassword(s.passwordHint ?: "")
            is TdApi.AuthorizationStateReady -> {
                _auth.value = AuthState.Ready
                send(TdApi.LoadChats(TdApi.ChatListMain(), 100))
            }
            is TdApi.AuthorizationStateLoggingOut,
            is TdApi.AuthorizationStateClosing,
            is TdApi.AuthorizationStateClosed -> {
                synchronized(chatMap) { chatMap.clear() }
                _chats.value = emptyList()
                _auth.value = AuthState.Loading
            }
            else -> _auth.value = AuthState.Unsupported(s.javaClass.simpleName)
        }
    }

    private fun sendParameters() {
        val dir = context.filesDir.absolutePath + "/td"
        send(TdApi.SetTdlibParameters().apply {
            useTestDc = false
            databaseDirectory = dir
            filesDirectory = "$dir/files"
            databaseEncryptionKey = ByteArray(0)
            useFileDatabase = true
            useChatInfoDatabase = true
            useMessageDatabase = true
            useSecretChats = false
            apiId = BuildConfig.TG_APP_ID
            apiHash = BuildConfig.TG_APP_HASH
            systemLanguageCode = Locale.getDefault().language.ifEmpty { "en" }
            deviceModel = Build.MODEL
            systemVersion = Build.VERSION.RELEASE
            applicationVersion = BuildConfig.VERSION_NAME
        })
    }

    private fun mutate(chatId: Long, block: (TdApi.Chat) -> Unit) {
        val chat = synchronized(chatMap) { chatMap[chatId] } ?: return
        synchronized(chat) { block(chat) }
        rebuild()
    }

    private fun mainPosition(c: TdApi.Chat): TdApi.ChatPosition? =
        c.positions.firstOrNull { it.list is TdApi.ChatListMain }

    private fun rebuild() {
        val items = synchronized(chatMap) {
            chatMap.values
                .mapNotNull { c -> mainPosition(c)?.takeIf { it.order != 0L }?.let { c to it } }
                .sortedByDescending { (_, p) -> p.order }
                .map { (c, p) ->
                    ChatItem(
                        id = c.id,
                        title = c.title,
                        preview = preview(c.lastMessage),
                        unread = c.unreadCount,
                        date = c.lastMessage?.date ?: 0,
                        pinned = p.isPinned,
                    )
                }
        }
        _chats.value = items
    }

    private fun sameList(a: TdApi.ChatList, b: TdApi.ChatList): Boolean = when {
        a is TdApi.ChatListMain && b is TdApi.ChatListMain -> true
        a is TdApi.ChatListArchive && b is TdApi.ChatListArchive -> true
        a is TdApi.ChatListFolder && b is TdApi.ChatListFolder -> a.chatFolderId == b.chatFolderId
        else -> false
    }

    private fun preview(m: TdApi.Message?): String = when (val c = m?.content) {
        null -> ""
        is TdApi.MessageText -> c.text.text
        is TdApi.MessagePhoto -> c.caption.text.ifEmpty { "Фото" }
        is TdApi.MessageVideo -> c.caption.text.ifEmpty { "Видео" }
        is TdApi.MessageAnimation -> "GIF"
        is TdApi.MessageAudio -> "Аудио"
        is TdApi.MessageVoiceNote -> "Голосовое сообщение"
        is TdApi.MessageDocument -> c.caption.text.ifEmpty { "Файл" }
        is TdApi.MessageSticker -> "${c.sticker.emoji} Стикер"
        else -> "Сообщение"
    }
}
