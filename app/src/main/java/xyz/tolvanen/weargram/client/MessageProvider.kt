package xyz.tolvanen.weargram.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.drinkless.td.libcore.telegram.TdApi
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

class MessageProvider @Inject constructor(val client: TelegramClient) {

    var chatId: Long = -1

    private val messageData = mutableMapOf<Long, TdApi.Message>()

    private val oldestMessageId = AtomicLong(-1L)

    fun getMessageData(messageId: Long): TdApi.Message? {
        return messageData[messageId]
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    fun getMessages(fromMessageId: Long, limit: Int): Flow<List<TdApi.Message>> =
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