package xyz.tolvanen.weargram.ui.message

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.client.TelegramClient
import javax.inject.Inject

@HiltViewModel
class MessageMenuViewModel @Inject constructor(private val client: TelegramClient) : ViewModel() {
    fun getMessage(chatId: Long, messageId: Long): Flow<TdApi.Message> {
        return client.sendRequest(TdApi.GetMessage(chatId, messageId)).filterIsInstance()
    }

    fun deleteMessage(chatId: Long, messageId: Long) {
        client.sendUnscopedRequest(TdApi.DeleteMessages(chatId, longArrayOf(messageId), true))
    }
}