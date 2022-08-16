package xyz.tolvanen.weargram.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import org.drinkless.td.libcore.telegram.TdApi

@Composable
fun MessageContent(message: TdApi.Message) {
    when (message.content) {
        is TdApi.MessageText -> Text((message.content as TdApi.MessageText).text.text, style = MaterialTheme.typography.body2)
        is TdApi.MessagePhoto -> Text("Photo")
        is TdApi.MessageAudio -> Text("Audio")
        is TdApi.MessageVideo -> Text("Video")
        is TdApi.MessageSticker -> Text((message.content as TdApi.MessageSticker).sticker.emoji + " Sticker")
        is TdApi.MessageDocument -> Text("file: " + (message.content as TdApi.MessageDocument).document.fileName)
        is TdApi.MessageAnimatedEmoji -> Text((message.content as TdApi.MessageAnimatedEmoji).emoji)
        is TdApi.MessageAnimation -> Text("Animation")
        is TdApi.MessageBasicGroupChatCreate -> Text("Group created")
        is TdApi.MessageCall -> Text("Call")
        is TdApi.MessagePoll -> Text("Call")
        else -> Text("Unsupported message")
    }

}

