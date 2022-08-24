package xyz.tolvanen.weargram.ui.chat

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.material.ScalingLazyListItemInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.*
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
    @ApplicationContext context: Context
) : ViewModel() {

    private val TAG = this::class.simpleName

    private val screenWidth = context.resources.displayMetrics.widthPixels

    private var _chatFlow = MutableStateFlow(TdApi.Chat())
    val chatFlow: StateFlow<TdApi.Chat> get() = _chatFlow

    fun initialize(chatId: Long) {
        messageProvider.initialize(chatId)
        pullMessages()

        chatProvider.chatData.onEach {
            it[chatId]?.also { chat ->
                _chatFlow.value = chat
            }
        }.launchIn(viewModelScope)
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
        // Take the smallest photo size whose width is larger than screen width,
        // or the largest available photo size if it doesn't exist
        val photoSize = photoMessage.photo.sizes.dropWhile { it.width < screenWidth }.firstOrNull()
            ?: photoMessage.photo.sizes.last()

        return fetchFile(photoSize.photo)
            .map {
                it?.let {
                    BitmapFactory.decodeFile(it)?.asImageBitmap()
                }
            }
    }
}

