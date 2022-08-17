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
                    updateChatPositions(it.chatId, arrayOf(it.position))
                }
                is TdApi.UpdateChatLastMessage -> {
                    chats[it.chatId]?.lastMessage = it.lastMessage
                    updateChatPositions(it.chatId, it.positions)
                }
                is TdApi.UpdateChatTitle -> {
                    chats[it.chatId]?.title = it.title
                    updateChats()
                }
                is TdApi.UpdateNewChat -> {
                    chats[it.chat.id] = it.chat
                    updateChatPositions(it.chat.id, it.chat.positions)
                }
                is TdApi.UpdateChatReadInbox -> {
                    chats[it.chatId]?.lastReadInboxMessageId = it.lastReadInboxMessageId
                    chats[it.chatId]?.unreadCount = it.unreadCount
                    updateChats()
                }
                is TdApi.UpdateChatReadOutbox -> {
                    chats[it.chatId]?.lastReadInboxMessageId = it.lastReadOutboxMessageId
                    updateChats()
                }
                is TdApi.UpdateChatPhoto -> {
                    chats[it.chatId]?.photo = it.photo
                    updateChats()
                }
                is TdApi.UpdateChatUnreadMentionCount -> {
                    chats[it.chatId]?.unreadMentionCount = it.unreadMentionCount
                    updateChats()
                }
                is TdApi.UpdateMessageMentionRead -> {
                    chats[it.chatId]?.unreadMentionCount = it.unreadMentionCount
                    updateChats()
                }
                is TdApi.UpdateChatReplyMarkup -> {
                    chats[it.chatId]?.replyMarkupMessageId = it.replyMarkupMessageId
                    updateChats()
                }
                is TdApi.UpdateChatDraftMessage -> {
                    chats[it.chatId]?.draftMessage = it.draftMessage
                    updateChatPositions(it.chatId, it.positions)
                }
                is TdApi.UpdateChatPermissions -> {
                    chats[it.chatId]?.permissions = it.permissions
                    updateChats()
                }
                is TdApi.UpdateChatNotificationSettings -> {
                    chats[it.chatId]?.notificationSettings = it.notificationSettings
                    updateChats()
                }
                is TdApi.UpdateChatDefaultDisableNotification -> {
                    chats[it.chatId]?.defaultDisableNotification = it.defaultDisableNotification
                    updateChats()
                }
                is TdApi.UpdateChatIsMarkedAsUnread -> {
                    chats[it.chatId]?.isMarkedAsUnread = it.isMarkedAsUnread
                    updateChats()
                }
                is TdApi.UpdateChatIsBlocked -> {
                    chats[it.chatId]?.isBlocked = it.isBlocked
                    updateChats()
                }
                is TdApi.UpdateChatHasScheduledMessages -> {
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
        //Log.d(TAG, "updatepositions: " + positions.joinToString { it.toString() })
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
