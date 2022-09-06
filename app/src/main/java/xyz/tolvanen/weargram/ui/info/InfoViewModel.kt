package xyz.tolvanen.weargram.ui.info

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.client.ChatProvider
import xyz.tolvanen.weargram.client.TelegramClient
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(
    private val client: TelegramClient,
    private val chatProvider: ChatProvider
) : ViewModel() {

    fun getUser(id: Long): TdApi.User? { return client.getUser(id) }
    fun fetchPhoto(photo: TdApi.ProfilePhoto): Flow<ImageBitmap?> {
        return client.getFilePath(photo.big).map {
            it?.let {
                BitmapFactory.decodeFile(it)?.asImageBitmap()
            }
        }
    }
}