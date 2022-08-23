package xyz.tolvanen.weargram.client

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.drinkless.td.libcore.telegram.TdApi
import javax.inject.Inject

class NotificationProvider @Inject constructor(private val client: TelegramClient) {

    private val TAG = this::class.simpleName

    private val scope = CoroutineScope(Dispatchers.Default)

    init {

        client.updateFlow
            .filterIsInstance<TdApi.UpdateNewMessage>()
            .onEach {
                Log.d(TAG, it.toString())

            }.launchIn(scope)

        client.updateFlow
            .filterIsInstance<TdApi.UpdateUnreadMessageCount>()
            .onEach {
                Log.d(TAG, it.toString())

            }.launchIn(scope)

        client.updateFlow
            .filterIsInstance<TdApi.UpdateUnreadChatCount>()
            .onEach {
                Log.d(TAG, it.toString())

            }.launchIn(scope)

    }
}