package xyz.tolvanen.weargram.client

import android.util.Log
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import java.lang.Thread.State
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.concurrent.withLock

class ChatProvider @Inject constructor(private val client: TelegramClient) {

    private val TAG = this::class.simpleName

    // remove of ConcurrentSkipListSet didn't work as expected. Instead, synchronize
    // access to non-threadsafe SortedSet with a ReentrantLock
    private val chatOrdering =
        sortedSetOf<Pair<Long, Long>>(comparator = { a, b -> if (a.second < b.second) 1 else -1 })
    private val chatOrderingLock = ReentrantLock()

    private val _chatIds = MutableStateFlow(listOf<Long>())
    val chatIds: StateFlow<List<Long>> get() = _chatIds

    private val _chatData = MutableStateFlow(persistentHashMapOf<Long, TdApi.Chat>())
    val chatData: StateFlow<PersistentMap<Long, TdApi.Chat>> get() = _chatData

    private val scope = CoroutineScope(Dispatchers.Default)

    private fun updateProperty(chatId: Long, update: (TdApi.Chat) -> TdApi.Chat) {
        _chatData.value[chatId]?.also {
            _chatData.value = _chatData.value.remove(chatId)
            _chatData.value = _chatData.value.put(chatId, update(it))
        }
    }

    init {

        client.updateFlow.onEach {
            //Log.d(TAG, it.toString())
            when (it) {
                is TdApi.UpdateChatPosition -> {
                    updateChatPositions(it.chatId, arrayOf(it.position))
                }
                is TdApi.UpdateChatLastMessage -> {

                    updateProperty(it.chatId) { chat ->
                        chat.apply { lastMessage = it.lastMessage }
                    }

                    updateChatPositions(it.chatId, it.positions)
                }
                is TdApi.UpdateChatTitle -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { title = it.title }
                    }
                    updateChats()
                }
                is TdApi.UpdateNewChat -> {
                    _chatData.value = _chatData.value.put(it.chat.id, it.chat)
                    updateChatPositions(it.chat.id, it.chat.positions)
                }
                is TdApi.UpdateChatReadInbox -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply {
                            lastReadInboxMessageId = it.lastReadInboxMessageId
                            unreadCount = it.unreadCount
                        }
                    }
                    updateChats()
                }
                is TdApi.UpdateChatReadOutbox -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { lastReadOutboxMessageId = it.lastReadOutboxMessageId }
                    }
                    updateChats()
                }
                is TdApi.UpdateChatPhoto -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { photo = it.photo }
                    }
                    updateChats()
                }
                is TdApi.UpdateChatUnreadMentionCount -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { unreadMentionCount = it.unreadMentionCount }
                    }
                    updateChats()
                }
                is TdApi.UpdateMessageMentionRead -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { unreadMentionCount = it.unreadMentionCount }
                    }
                    updateChats()
                }
                is TdApi.UpdateChatReplyMarkup -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { replyMarkupMessageId = it.replyMarkupMessageId }
                    }
                    updateChats()
                }
                is TdApi.UpdateChatDraftMessage -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { draftMessage = it.draftMessage }
                    }
                    updateChatPositions(it.chatId, it.positions)
                }
                is TdApi.UpdateChatPermissions -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { permissions = it.permissions }
                    }
                    updateChats()
                }
                is TdApi.UpdateChatNotificationSettings -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { notificationSettings = it.notificationSettings }
                    }
                    updateChats()
                }
                is TdApi.UpdateChatDefaultDisableNotification -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { defaultDisableNotification = it.defaultDisableNotification }
                    }
                    updateChats()
                }
                is TdApi.UpdateChatIsMarkedAsUnread -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { isMarkedAsUnread = it.isMarkedAsUnread }
                    }
                    updateChats()
                }
                is TdApi.UpdateChatIsBlocked -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { isBlocked = it.isBlocked }
                    }
                    updateChats()
                }
                is TdApi.UpdateChatHasScheduledMessages -> {
                    updateProperty(it.chatId) { chat ->
                        chat.apply { hasScheduledMessages = it.hasScheduledMessages }
                    }
                    updateChats()
                }
                //is TdApi.UpdateChatFilters -> {}
                //is TdApi.UpdateChatHasProtectedContent -> {}
                //is TdApi.UpdateChatMember -> {}
                //is TdApi.UpdateChatMessageSender -> {}
                //is TdApi.UpdateChatMessageTtl -> {}
                //is TdApi.UpdateChatOnlineMemberCount -> {}
                //is TdApi.UpdateChatPendingJoinRequests -> {}
                //is TdApi.UpdateChatTheme -> {}
                //is TdApi.UpdateChatThemes -> {}
                //is TdApi.UpdateChatVideoChat -> {}
                // TODO: Make sure message content updates of last messages are updated here, too

            }
        }.launchIn(scope)
    }

    fun loadChats() {
        scope.launch {
            client.sendRequest(TdApi.LoadChats(TdApi.ChatListMain(), Int.MAX_VALUE))
                .collect {}
        }
    }

    private fun updateChats() {
        chatOrderingLock.withLock { _chatIds.value = chatOrdering.toList().map { it.first } }
    }

    private fun updateChatPositions(chatId: Long, positions: Array<TdApi.ChatPosition>) {
        chatOrderingLock.withLock {
            chatOrdering.removeIf { it.first == chatId }
            positions.dropWhile { it.list !is TdApi.ChatListMain }
                .firstOrNull()?.order?.also { order ->
                    chatOrdering.add(Pair(chatId, order))
                }
        }
        updateChats()
    }

    fun getChat(chatId: Long): TdApi.Chat? {
        return _chatData.value[chatId]
    }
}
