package xyz.tolvanen.weargram.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Icon
import org.drinkless.td.libcore.telegram.TdApi

@Composable
fun MessageStatusIcon(message: TdApi.Message, chat: TdApi.Chat, modifier: Modifier = Modifier) {

    if (message.isOutgoing) {
        when (message.sendingState) {
            is TdApi.MessageSendingStatePending -> {
                Icon(imageVector = Icons.Outlined.Pending, contentDescription = null, modifier)
            }
            is TdApi.MessageSendingStateFailed -> {
                Icon(imageVector = Icons.Outlined.SyncProblem, contentDescription = null, modifier)
            }
            else -> {
                val lastReadId = chat.lastReadOutboxMessageId
                if ((message.interactionInfo?.viewCount ?: 0) > 0 || lastReadId >= message.id) {
                    Icon(imageVector = Icons.Outlined.DoneAll, contentDescription = null, modifier)
                } else {
                    Icon(imageVector = Icons.Outlined.Done, contentDescription = null, modifier)
                }
            }
        }
    }
}