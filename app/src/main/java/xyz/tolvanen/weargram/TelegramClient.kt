package xyz.tolvanen.weargram

import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.AuthorizationStateWaitOtherDeviceConfirmation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.concurrent.withLock


class TelegramClient @Inject constructor(private val parameters: TdApi.TdlibParameters) {

    private val _authorizationState = MutableStateFlow(Authorization.UNAUTHORIZED)
    val authorizationState: StateFlow<Authorization> get() = _authorizationState

    private val _linkState = MutableStateFlow<String?>(null)
    val linkState: StateFlow<String?> get() = _linkState

    val chats = ConcurrentHashMap<Long, TdApi.Chat>()

    // remove of ConcurrentSkipListSet didn't work as expected. Instead, synchronize
    // access to non-threadsafe SortedSet with a ReentrantLock
    val chatOrdering =
        sortedSetOf<Pair<Long, Long>>(comparator = { a, b -> if (a.second < b.second) 1 else -1 })
    val chatOrderingLock = ReentrantLock()

    val chatFlow = MutableSharedFlow<List<TdApi.Chat>>()

    val newMessageFlow = MutableSharedFlow<TdApi.Message>()
    val deleteMessagesFlow = MutableSharedFlow<TdApi.UpdateDeleteMessages>()
    val messageUpdateFlow = MutableSharedFlow<TdApi.UpdateMessageContent>()
    val messageSendSucceededFlow = MutableSharedFlow<TdApi.UpdateMessageSendSucceeded>()

    val users = ConcurrentHashMap<Long, TdApi.User>()
    val basicGroups = ConcurrentHashMap<Long, TdApi.BasicGroup>()
    val supergroups = ConcurrentHashMap<Long, TdApi.Supergroup>()

    val userInfos = ConcurrentHashMap<Long, TdApi.UserFullInfo>()
    val basicGroupInfos = ConcurrentHashMap<Long, TdApi.BasicGroupFullInfo>()
    val supergroupInfos = ConcurrentHashMap<Long, TdApi.SupergroupFullInfo>()

    private val TAG = "TelegramClient"

    private val resultHandler = Client.ResultHandler {
        when (it.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                Log.d(TAG, "UpdateAuthorizationState")
                onAuthorizationState(it as TdApi.UpdateAuthorizationState)
            }
            TdApi.UpdateUser.CONSTRUCTOR -> {
                Log.d(TAG, "user: $it")
                val update = it as TdApi.UpdateUser
                users[update.user.id] = update.user
            }
            TdApi.UpdateUserStatus.CONSTRUCTOR -> {
                Log.d(TAG, "user status: $it")
                val update = it as TdApi.UpdateUserStatus
                users[update.userId]?.status = update.status
            }
            TdApi.UpdateBasicGroup.CONSTRUCTOR -> {
                Log.d(TAG, "basic group: $it")
                val update = it as TdApi.UpdateBasicGroup
                basicGroups[update.basicGroup.id] = update.basicGroup
            }
            TdApi.UpdateSupergroup.CONSTRUCTOR -> {
                Log.d(TAG, "supergroup: $it")
                val update = it as TdApi.UpdateSupergroup
                supergroups[update.supergroup.id] = update.supergroup
            }
            TdApi.UpdateUserFullInfo.CONSTRUCTOR -> {
                Log.d(TAG, "userFullInfo: $it")
                val update = it as TdApi.UpdateUserFullInfo
                userInfos[update.userId] = update.userFullInfo
            }
            TdApi.UpdateBasicGroupFullInfo.CONSTRUCTOR -> {
                Log.d(TAG, "bgFullInfo: $it")
                val update = it as TdApi.UpdateBasicGroupFullInfo
                basicGroupInfos[update.basicGroupId] = update.basicGroupFullInfo
            }
            TdApi.UpdateSupergroupFullInfo.CONSTRUCTOR -> {
                Log.d(TAG, "sgFullInfo: $it")
                val update = it as TdApi.UpdateSupergroupFullInfo
                supergroupInfos[update.supergroupId] = update.supergroupFullInfo
            }
            TdApi.UpdateChatPosition.CONSTRUCTOR -> {
                Log.d(TAG, "chatPosition: $it")
                val update = it as TdApi.UpdateChatPosition
                updateChatPositions(update.chatId, arrayOf(update.position))
                updateChats()
            }
            TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
                Log.d(TAG, "chatLastMessage: $it")
                val update = it as TdApi.UpdateChatLastMessage

                chats[update.chatId]?.lastMessage = update.lastMessage
                updateChatPositions(update.chatId, update.positions)
                updateChats()
            }
            TdApi.UpdateChatTitle.CONSTRUCTOR -> {
                Log.d(TAG, "chatTitle: $it")
                val update = it as TdApi.UpdateChatTitle
                chats[update.chatId]?.title = update.title
                updateChats()
            }
            TdApi.UpdateNewChat.CONSTRUCTOR -> {
                Log.d(TAG, "newChat: $it")
                val update = it as TdApi.UpdateNewChat
                chats[update.chat.id] = update.chat
                updateChatPositions(update.chat.id, update.chat.positions)
                updateChats()
            }
            TdApi.UpdateChatReadInbox.CONSTRUCTOR -> {
                Log.d(TAG, "chatReadInbox: $it")
                val update = it as TdApi.UpdateChatReadInbox
                chats[update.chatId]?.lastReadInboxMessageId = update.lastReadInboxMessageId
                chats[update.chatId]?.unreadCount = update.unreadCount
                updateChats()
            }
            TdApi.UpdateChatReadOutbox.CONSTRUCTOR -> {
                Log.d(TAG, "chatReadOutbox: $it")
                val update = it as TdApi.UpdateChatReadOutbox
                chats[update.chatId]?.lastReadInboxMessageId = update.lastReadOutboxMessageId
                updateChats()
            }
            TdApi.UpdateChatPhoto.CONSTRUCTOR -> {
                Log.d(TAG, "chatPhoto: $it")
                val update = it as TdApi.UpdateChatPhoto
                chats[update.chatId]?.photo = update.photo
                updateChats()
            }
            TdApi.UpdateChatUnreadMentionCount.CONSTRUCTOR -> {
                Log.d(TAG, "chatUnreadMentionCount: $it")
                val update = it as TdApi.UpdateChatUnreadMentionCount
                chats[update.chatId]?.unreadMentionCount = update.unreadMentionCount
                updateChats()
            }
            TdApi.UpdateMessageMentionRead.CONSTRUCTOR -> {
                Log.d(TAG, "messageMentionRead: $it")
                val update = it as TdApi.UpdateMessageMentionRead
                chats[update.chatId]?.unreadMentionCount = update.unreadMentionCount
                updateChats()
            }
            TdApi.UpdateChatReplyMarkup.CONSTRUCTOR -> {
                Log.d(TAG, "chatReplyMarkup: $it")
                val update = it as TdApi.UpdateChatReplyMarkup
                chats[update.chatId]?.replyMarkupMessageId = update.replyMarkupMessageId
                updateChats()
            }
            TdApi.UpdateChatDraftMessage.CONSTRUCTOR -> {
                Log.d(TAG, "chatDraftMessage: $it")
                val update = it as TdApi.UpdateChatDraftMessage
                chats[update.chatId]?.draftMessage = update.draftMessage
                updateChatPositions(update.chatId, update.positions)
                updateChats()
            }
            TdApi.UpdateChatPermissions.CONSTRUCTOR -> {
                Log.d(TAG, "chatPermissions: $it")
                val update = it as TdApi.UpdateChatPermissions
                chats[update.chatId]?.permissions = update.permissions
                updateChats()
            }
            TdApi.UpdateChatNotificationSettings.CONSTRUCTOR -> {
                Log.d(TAG, "chatNotificationSettings: $it")
                val update = it as TdApi.UpdateChatNotificationSettings
                chats[update.chatId]?.notificationSettings = update.notificationSettings
                updateChats()
            }
            TdApi.UpdateChatDefaultDisableNotification.CONSTRUCTOR -> {
                Log.d(TAG, "chatDefaultDisableNotification: $it")
                val update = it as TdApi.UpdateChatDefaultDisableNotification
                chats[update.chatId]?.defaultDisableNotification = update.defaultDisableNotification
                updateChats()
            }
            TdApi.UpdateChatIsMarkedAsUnread.CONSTRUCTOR -> {
                Log.d(TAG, "chatIsMarkedAsUnread: $it")
                val update = it as TdApi.UpdateChatIsMarkedAsUnread
                chats[update.chatId]?.isMarkedAsUnread = update.isMarkedAsUnread
                updateChats()
            }
            TdApi.UpdateChatIsBlocked.CONSTRUCTOR -> {
                Log.d(TAG, "chatIsBlocked: $it")
                val update = it as TdApi.UpdateChatIsBlocked
                chats[update.chatId]?.isBlocked = update.isBlocked
                updateChats()
            }
            TdApi.UpdateChatHasScheduledMessages.CONSTRUCTOR -> {
                Log.d(TAG, "chatHasSchedulesMessages: $it")
                val update = it as TdApi.UpdateChatHasScheduledMessages
                chats[update.chatId]?.hasScheduledMessages = update.hasScheduledMessages
                updateChats()
            }
            TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                Log.d(TAG, "new message: $it")
                val update = it as TdApi.UpdateNewMessage
                mainScope.launch {
                    newMessageFlow.emit(update.message)
                }
            }
            TdApi.UpdateMessageContent.CONSTRUCTOR -> {
                Log.d(TAG, "messageContent: $it")
                val update = it as TdApi.UpdateMessageContent
                mainScope.launch {
                    messageUpdateFlow.emit(update)
                }
            }
            TdApi.UpdateDeleteMessages.CONSTRUCTOR -> {
                Log.d(TAG, "deleteMessages: $it")
                val update = it as TdApi.UpdateDeleteMessages
                if (update.isPermanent) {
                    mainScope.launch {
                        deleteMessagesFlow.emit(update)
                    }
                }
            }
            TdApi.UpdateMessageSendSucceeded.CONSTRUCTOR -> {
                Log.d(TAG, "messageSendSucceeded: $it")
                val update = it as TdApi.UpdateMessageSendSucceeded
                mainScope.launch {
                    messageSendSucceededFlow.emit(update)
                }
            }
        }
    }

    private fun updateChats() {
        defaultScope.launch {
            chatFlow.emit(chatOrderingLock.withLock {
                chatOrdering.mapNotNull { chats[it.first] }
            })
        }
    }

    private fun updateChatPositions(chatId: Long, positions: Array<TdApi.ChatPosition>) {
        chatOrderingLock.withLock {
            chatOrdering.removeIf { it.first == chatId }
            chatOrdering.add(Pair(chatId,
                positions.dropWhile { it.list.constructor != TdApi.ChatListMain.CONSTRUCTOR }
                    .first().order
                )
            )
        }
    }

    private fun onAuthorizationState(authorizationUpdate: TdApi.UpdateAuthorizationState) {

        when (authorizationUpdate.authorizationState.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                Log.d( TAG, "onResult: AuthorizationStateWaitTdlibParameters -> state = UNAUTHENTICATED")
                _authorizationState.value = Authorization.UNAUTHORIZED
            }
            TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitEncryptionKey")
                client.send(TdApi.CheckDatabaseEncryptionKey()) {

                }
            }
            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitPhoneNumber -> state = WAIT_NUMBER")
                client.send(TdApi.RequestQrCodeAuthentication(), resultHandler)
                _authorizationState.value = Authorization.WAIT_NUMBER
            }
            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitCode -> state = WAIT_CODE")
                _authorizationState.value = Authorization.WAIT_CODE
            }
            TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitPassword")
                _authorizationState.value = Authorization.WAIT_PASSWORD
            }
            TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitOtherDeviceConfirmation")
                _authorizationState.value = Authorization.WAIT_NUMBER
                val link = (authorizationUpdate.authorizationState as AuthorizationStateWaitOtherDeviceConfirmation).link
                _linkState.value = link
                Log.d(TAG, link)
                //_authorizationState.value = Authorization.WAIT_

            }
            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateReady -> state = AUTHENTICATED")
                _authorizationState.value = Authorization.AUTHORIZED
                client.send(TdApi.LoadChats(TdApi.ChatListMain(), Int.MAX_VALUE)) {
                    Log.d("top", "here!!")

                }
            }
            TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateLoggingOut")
                _authorizationState.value = Authorization.UNAUTHORIZED
            }
        }
    }

    val client: Client = Client.create(resultHandler, null, null)

    init {
        client.send(TdApi.SetLogVerbosityLevel(0), resultHandler)
        client.send(TdApi.GetAuthorizationState(), resultHandler)

    }

    private val requestScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val defaultScope = CoroutineScope(Dispatchers.Default)

    fun startAuthorization() {
        requestScope.launch {
            client.send(TdApi.SetTdlibParameters(parameters)) {
            }
        }
    }

    fun setPhoneNumber(phoneNumber: String) {
        Log.d(TAG, "phoneNumber: $phoneNumber")
        val settings = TdApi.PhoneNumberAuthenticationSettings()

        requestScope.launch {
            client.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings)) {
                Log.d(TAG, "phoneNumber. result: $it")
                when (it.constructor) {
                    TdApi.Ok.CONSTRUCTOR -> {
                        Log.d(TAG, "phone number ok")
                    }
                    TdApi.Error.CONSTRUCTOR -> {
                        Log.d(TAG, "phone number error")
                        _authorizationState.value = Authorization.INVALID_NUMBER
                    }
                }
            }

        }
    }

    fun setCode(code: String) {
        Log.d(TAG, "code: $code")
        requestScope.launch {
            client.send(TdApi.CheckAuthenticationCode(code)) {
                when (it.constructor) {
                    TdApi.Ok.CONSTRUCTOR -> {
                        Log.d(TAG, "code ok")
                    }
                    TdApi.Error.CONSTRUCTOR -> {
                        Log.d(TAG, "code error")
                        _authorizationState.value = Authorization.INVALID_CODE
                    }
                }
            }
        }
    }

    fun setPassword(password: String) {
        Log.d(TAG, "password: $password")
        requestScope.launch {
            client.send(TdApi.CheckAuthenticationPassword(password)) {
                when (it.constructor) {
                    TdApi.Ok.CONSTRUCTOR -> {
                        Log.d(TAG, "password ok")
                    }
                    TdApi.Error.CONSTRUCTOR -> {
                        Log.d(TAG, "password error")
                        _authorizationState.value = Authorization.INVALID_PASSWORD
                    }
                }
            }
        }
    }

    fun getChat(chatId: Long): TdApi.Chat? {
        return chats[chatId]
    }
}

enum class Authorization {
    UNAUTHORIZED,
    WAIT_NUMBER,
    INVALID_NUMBER,
    WAIT_CODE,
    INVALID_CODE,
    WAIT_PASSWORD,
    INVALID_PASSWORD,
    AUTHORIZED,
}
