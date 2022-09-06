package xyz.tolvanen.weargram.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.client.ChatProvider
import xyz.tolvanen.weargram.client.TelegramClient
import javax.inject.Inject

@HiltViewModel
class ChatMenuViewModel @Inject constructor(
    private val client: TelegramClient,
    private val chatProvider: ChatProvider
) : ViewModel() {
    fun getChat(chatId: Long): TdApi.Chat? {
        return chatProvider.getChat(chatId)
    }

}
