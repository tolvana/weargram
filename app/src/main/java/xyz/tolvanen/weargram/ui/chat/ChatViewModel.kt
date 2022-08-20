package xyz.tolvanen.weargram.ui.chat

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.wear.compose.material.ScalingLazyListItemInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.client.ChatProvider
import xyz.tolvanen.weargram.client.MessageProvider
import xyz.tolvanen.weargram.client.TelegramClient
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val client: TelegramClient,
    private val chatProvider: ChatProvider,
    val messageProvider: MessageProvider,
) : ViewModel() {

    private val TAG = this::class.simpleName

    fun initialize(chatId: Long) {
        messageProvider.initialize(chatId)
        pullMessages()
    }

    fun pullMessages() {
        messageProvider.pullMessages()
    }

    fun sendMessageAsync(content: TdApi.InputMessageContent): Deferred<TdApi.Message> {
        return messageProvider.sendMessageAsync(0, 0, TdApi.MessageSendOptions(), content)
    }

    fun onStart(chatId: Long) {
        client.sendUnscopedRequest(TdApi.OpenChat(chatId))
    }

    fun onStop(chatId: Long) {
        client.sendUnscopedRequest(TdApi.CloseChat(chatId))
    }

    fun updateVisibleItems(visibleItems: List<ScalingLazyListItemInfo>) {
        messageProvider.updateSeenItems(
            visibleItems.map { it.key }.filterIsInstance<Long>()
        )
    }

    fun fetchFile(file: TdApi.File): Flow<String?> {
        return client.getFilePath(file)
    }

    fun fetchPhoto(photoMessage: TdApi.MessagePhoto): Flow<ImageBitmap?> {
        return fetchFile(photoMessage.photo.sizes.last().photo)
            .map {
                it?.let {
                    BitmapFactory.decodeFile(it)?.asImageBitmap()
                }
            }
    }

}

