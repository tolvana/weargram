package xyz.tolvanen.weargram.client

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.TelegramClient
import javax.inject.Inject

class ChatProvider @Inject constructor(private val telegramClient: TelegramClient) {

    private fun getChatIds(limit: Int): Flow<LongArray> =
        callbackFlow {
            telegramClient.client.send(TdApi.LoadChats(TdApi.ChatListMain(), limit)) {
                when (it.constructor) {
                    TdApi.Chats.CONSTRUCTOR -> {
                        trySend((it as TdApi.Chats).chatIds)
                    }
                    TdApi.Error.CONSTRUCTOR -> {
                        Log.d("topkek", "all is loaded")
                    }
                }
            }
            awaitClose { }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getChats(limit: Int): Flow<List<TdApi.Chat>> =
        getChatIds(limit)
            .map { ids -> ids.map { getChat(it) } }
            .flatMapLatest { chatsFlow ->
                combine(chatsFlow) { chats ->
                    Log.d("topkek", "limit: $limit")
                    Log.d("topkek", chats.joinToString { it.title })
                    chats.toList()
                }
            }

    fun getChat(chatId: Long): Flow<TdApi.Chat> = callbackFlow {
        telegramClient.client.send(TdApi.GetChat(chatId)) {
            when (it.constructor) {
                TdApi.Chat.CONSTRUCTOR -> {
                    trySend(it as TdApi.Chat)
                }
                TdApi.Error.CONSTRUCTOR -> {
                }
            }
        }
        awaitClose { }

    }
}
