package xyz.tolvanen.weargram.client

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.concurrent.withLock

class ChatProvider @Inject constructor(private val client: TelegramClient) {

    private val TAG = this::class.simpleName
    val chats = ConcurrentHashMap<Long, TdApi.Chat>()

    // remove of ConcurrentSkipListSet didn't work as expected. Instead, synchronize
    // access to non-threadsafe SortedSet with a ReentrantLock
    val chatOrdering =
        sortedSetOf<Pair<Long, Long>>(comparator = { a, b -> if (a.second < b.second) 1 else -1 })
    val chatOrderingLock = ReentrantLock()

    private val _chatFlow = MutableSharedFlow<List<TdApi.Chat>>()
    val chatFlow: SharedFlow<List<TdApi.Chat>> get() = _chatFlow

    private val scope = CoroutineScope(Dispatchers.Default)

    init {

        Log.d(TAG, "init")

        client.updateFlow.onEach {
            when (it) {
                is TdApi.UpdateChatPosition -> {
                    Log.d(TAG, "chatPosition: $it")
                    updateChatPositions(it.chatId, arrayOf(it.position))
                }
                is TdApi.UpdateChatLastMessage -> {
                    Log.d(TAG, "chatLastMessage: $it")
                    chats[it.chatId]?.lastMessage = it.lastMessage
                    updateChatPositions(it.chatId, it.positions)
                }
                is TdApi.UpdateChatTitle -> {
                    Log.d(TAG, "chatTitle: $it")
                    chats[it.chatId]?.title = it.title
                    updateChats()
                }
                is TdApi.UpdateNewChat -> {
                    Log.d(TAG, "newChat: $it")
                    chats[it.chat.id] = it.chat
                    updateChatPositions(it.chat.id, it.chat.positions)
                }
                is TdApi.UpdateChatReadInbox -> {
                    Log.d(TAG, "chatReadInbox: $it")
                    chats[it.chatId]?.lastReadInboxMessageId = it.lastReadInboxMessageId
                    chats[it.chatId]?.unreadCount = it.unreadCount
                    updateChats()
                }
                is TdApi.UpdateChatReadOutbox -> {
                    Log.d(TAG, "chatReadOutbox: $it")
                    chats[it.chatId]?.lastReadInboxMessageId = it.lastReadOutboxMessageId
                    updateChats()
                }
                is TdApi.UpdateChatPhoto -> {
                    Log.d(TAG, "chatPhoto: $it")
                    chats[it.chatId]?.photo = it.photo
                    updateChats()
                }
                is TdApi.UpdateChatUnreadMentionCount -> {
                    Log.d(TAG, "chatUnreadMentionCount: $it")
                    chats[it.chatId]?.unreadMentionCount = it.unreadMentionCount
                    updateChats()
                }
                is TdApi.UpdateMessageMentionRead -> {
                    Log.d(TAG, "messageMentionRead: $it")
                    chats[it.chatId]?.unreadMentionCount = it.unreadMentionCount
                    updateChats()
                }
                is TdApi.UpdateChatReplyMarkup -> {
                    Log.d(TAG, "chatReplyMarkup: $it")
                    chats[it.chatId]?.replyMarkupMessageId = it.replyMarkupMessageId
                    updateChats()
                }
                is TdApi.UpdateChatDraftMessage -> {
                    Log.d(TAG, "chatDraftMessage: $it")
                    chats[it.chatId]?.draftMessage = it.draftMessage
                    updateChatPositions(it.chatId, it.positions)
                }
                is TdApi.UpdateChatPermissions -> {
                    Log.d(TAG, "chatPermissions: $it")
                    chats[it.chatId]?.permissions = it.permissions
                    updateChats()
                }
                is TdApi.UpdateChatNotificationSettings -> {
                    Log.d(TAG, "chatNotificationSettings: $it")
                    chats[it.chatId]?.notificationSettings = it.notificationSettings
                    updateChats()
                }
                is TdApi.UpdateChatDefaultDisableNotification -> {
                    Log.d(TAG, "chatDefaultDisableNotification: $it")
                    chats[it.chatId]?.defaultDisableNotification = it.defaultDisableNotification
                    updateChats()
                }
                is TdApi.UpdateChatIsMarkedAsUnread -> {
                    Log.d(TAG, "chatIsMarkedAsUnread: $it")
                    chats[it.chatId]?.isMarkedAsUnread = it.isMarkedAsUnread
                    updateChats()
                }
                is TdApi.UpdateChatIsBlocked -> {
                    Log.d(TAG, "chatIsBlocked: $it")
                    chats[it.chatId]?.isBlocked = it.isBlocked
                    updateChats()
                }
                is TdApi.UpdateChatHasScheduledMessages -> {
                    Log.d(TAG, "chatHasSchedulesMessages: $it")
                    chats[it.chatId]?.hasScheduledMessages = it.hasScheduledMessages
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
        scope.launch {
            _chatFlow.emit(chatOrderingLock.withLock {
                chatOrdering.mapNotNull { chats[it.first] }
            })
        }
    }

    private fun updateChatPositions(chatId: Long, positions: Array<TdApi.ChatPosition>) {
        Log.d(TAG, "updatepositions: " + positions.joinToString { it.toString() })
        chatOrderingLock.withLock {
            chatOrdering.removeIf { it.first == chatId }
            chatOrdering.add(Pair(chatId,
                positions.dropWhile { it.list.constructor != TdApi.ChatListMain.CONSTRUCTOR }
                    .firstOrNull()?.order ?: 0
            )
            )
        }
        updateChats()
    }

    fun getChat(chatId: Long): TdApi.Chat? {
        return chats[chatId]
    }

}
