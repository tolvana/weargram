package xyz.tolvanen.weargram.client

import android.util.Log
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.drinkless.td.libcore.telegram.TdApi
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

class MessageProvider @Inject constructor(
    private val client: TelegramClient,
    ) {

    private val TAG = this::class.simpleName

    private var chatId: Long = -1

    private val oldestMessageId = AtomicLong(0)
    private val lastQueriedMessageId = AtomicLong(-1)

    private val _messageIds = MutableStateFlow(persistentListOf<Long>())
    private val _messageData = MutableStateFlow(persistentHashMapOf<Long, TdApi.Message>())

    val messageIds: StateFlow<PersistentList<Long>> get() = _messageIds
    val messageData: StateFlow<PersistentMap<Long, TdApi.Message>> get() = _messageData

    private val scope = CoroutineScope(Dispatchers.Default)

    fun initialize(chatId: Long) {
        this.chatId = chatId

        client.updateFlow
            .filterIsInstance<TdApi.UpdateNewMessage>()
            .filter { it.message.chatId == chatId }
            .onEach {
                _messageData.value = _messageData.value.put(it.message.id, it.message)
                _messageIds.value = _messageIds.value.add(0, it.message.id)
            }.launchIn(scope)

        client.updateFlow
            .filterIsInstance<TdApi.UpdateDeleteMessages>()
            .filter { it.chatId == chatId }
            .filter { it.isPermanent }
            .onEach {
                _messageIds.value = _messageIds.value.removeAll(it.messageIds.toList())
                it.messageIds.forEach { id -> _messageData.value = _messageData.value.remove(id) }
            }.launchIn(scope)

        client.updateFlow
            .filterIsInstance<TdApi.UpdateMessageContent>()
            .filter { it.chatId == chatId }
            .onEach {
                _messageData.value[it.messageId]?.also { msg ->
                    msg.content = it.newContent
                    _messageData.value = _messageData.value.remove(it.messageId)
                    _messageData.value = _messageData.value.put(it.messageId, msg)
                }
            }.launchIn(scope)

        client.updateFlow
            .filterIsInstance<TdApi.UpdateMessageSendSucceeded>()
            .filter { it.message.chatId == chatId }
            .onEach {
                _messageIds.value = _messageIds.value.mutate { list ->
                    list[_messageIds.value.indexOf(it.oldMessageId)] = it.message.id
                }
                _messageData.value = _messageData.value.put(it.message.id, it.message)
                _messageData.value = _messageData.value.remove(it.oldMessageId)
            }.launchIn(scope)

    }

    fun pullMessages() {
        if (lastQueriedMessageId.get() != oldestMessageId.get()) {
            val msgId = oldestMessageId.get()
            lastQueriedMessageId.set(msgId)

            val messageSource = getMessages(msgId, limit = 10)
            scope.launch {
                messageSource.collect { messages ->
                    Log.d(TAG, "got ${messages.size} messages")
                    _messageData.value = _messageData.value.putAll(messages.associateBy { message -> message.id })
                    _messageIds.value = _messageIds.value.addAll(messages.map { message -> message.id })

                    _messageIds.value.lastOrNull()?.also { id ->
                        if (oldestMessageId.get() != id) {
                            oldestMessageId.set(id)
                        }
                    }
                }
            }
        }

    }

    private fun getMessages(fromMessageId: Long, limit: Int): Flow<List<TdApi.Message>> =
        client.sendRequest(TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false))
            .filterIsInstance<TdApi.Messages>()
            .map { it.messages.toList() }


    fun sendMessageAsync(
        messageThreadId: Long = 0,
        replyToMessageId: Long = 0,
        options: TdApi.MessageSendOptions = TdApi.MessageSendOptions(),
        inputMessageContent: TdApi.InputMessageContent
    ): Deferred<TdApi.Message> = sendMessageAsync(
        TdApi.SendMessage(
            chatId,
            messageThreadId,
            replyToMessageId,
            options,
            null,
            inputMessageContent
        )
    )

    private fun sendMessageAsync(sendMessage: TdApi.SendMessage): Deferred<TdApi.Message> {
        val result = CompletableDeferred<TdApi.Message>()
        scope.launch {
            client.sendRequest(sendMessage).filterIsInstance<TdApi.Message>().collect {
                result.complete(it)
            }
        }
        return result
    }
}