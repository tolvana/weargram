package xyz.tolvanen.weargram.ui.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
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
import java.lang.Float.max
import java.lang.Float.min
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sign

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

    fun getUser(id: Long): TdApi.User? = client.getUser(id)
    fun getBasicGroup(id: Long): TdApi.BasicGroup? = client.getBasicGroup(id)
    fun getSupergroup(id: Long): TdApi.Supergroup? = client.getSupergroup(id)

    fun getUserInfo(id: Long): TdApi.UserFullInfo? = client.getUserInfo(id)
    fun getBasicGroupInfo(id: Long): TdApi.BasicGroupFullInfo? = client.getBasicGroupInfo(id)
    fun getSupergroupInfo(id: Long): TdApi.SupergroupFullInfo? = client.getSupergroupInfo(id)

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

        return fetchFile(photoSize.photo).map {
            it?.let {
                BitmapFactory.decodeFile(it)?.asImageBitmap()
            }
        }
    }

    fun fetchAudio(content: TdApi.File): Flow<MediaPlayer?> {
        return fetchFile(content).map {
            it?.let {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(it)
                    prepare()
                }
            }
        }
    }

    private var _scrollDirectionFlow = MutableStateFlow(1)
    val scrollDirectionFlow: StateFlow<Int> get() = _scrollDirectionFlow

    private var scrollOffset = 0f
    private val threshold = 50f

    val scrollListener = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y > 0) {
                scrollOffset = max(scrollOffset, 0f)

            } else if (available.y < 0) {
                scrollOffset = min(scrollOffset, 0f)
            }

            scrollOffset += available.y

            if (abs(scrollOffset) > threshold) {
                _scrollDirectionFlow.value = sign(scrollOffset).toInt()
            }

            return super.onPreScroll(available, source)
        }
    }

}

