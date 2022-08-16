package xyz.tolvanen.weargram.client

import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import xyz.tolvanen.weargram.TelegramClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    fun getMessages(fromMessageId: Long, limit: Int): Flow<List<TdApi.Message>> =
        callbackFlow {
            client.client.send(TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false)) {
                when (it.constructor) {
                    TdApi.Messages.CONSTRUCTOR -> {
                        (it as TdApi.Messages).messages.also { messages ->
                            Log.d("MessageProvider", "got ${messages.size} messages")
                            Log.d("MessageProvider", messages.joinToString { it.id.toString() })
                            trySend(messages.toList())
                        }
                    }
                    TdApi.Error.CONSTRUCTOR -> {
                        error("")
                    }
                    else -> {
                        error("")
                    }
                }
            }
            awaitClose { }
        }

    //fun getMessageIdsPaged(): PagingSource<Long, Long> =
    //    MessagePagingSource(chatId, this)

    fun getMessage(messageId: Long): Flow<TdApi.Message> = callbackFlow {
        client.client.send(TdApi.GetMessage(chatId, messageId)) {
            when (it.constructor) {
                TdApi.Message.CONSTRUCTOR -> {
                    trySend(it as TdApi.Message)
                }
                TdApi.Error.CONSTRUCTOR -> {
                    error("Something went wrong")
                }
                else -> {
                    error("Something went wrong")
                }
            }
        }
        awaitClose { }
    }

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

    fun sendMessageAsync(sendMessage: TdApi.SendMessage): Deferred<TdApi.Message> {
        val result = CompletableDeferred<TdApi.Message>()
        client.client.send(sendMessage) {
            when (it.constructor) {
                TdApi.Message.CONSTRUCTOR -> {
                    result.complete(it as TdApi.Message)
                }
                else -> {
                    result.completeExceptionally(error("Something went wrong"))
                }
            }
        }
        return result
    }
}