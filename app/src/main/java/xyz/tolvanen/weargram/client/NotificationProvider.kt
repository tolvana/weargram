package xyz.tolvanen.weargram.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.MessageSenderUser
import xyz.tolvanen.weargram.MainActivity
import xyz.tolvanen.weargram.R
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class NotificationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatProvider: ChatProvider,
    private val client: TelegramClient
) {

    private val TAG = this::class.simpleName

    private val scope = CoroutineScope(Dispatchers.Default)

    private val groups: ConcurrentHashMap<Int, TdApi.NotificationGroup> =
        ConcurrentHashMap<Int, TdApi.NotificationGroup>()

    init {

        client.updateFlow
            .filterIsInstance<TdApi.UpdateActiveNotifications>()
            .onEach { updateActiveNotifications(it) }.launchIn(scope)

        client.updateFlow
            .filterIsInstance<TdApi.UpdateNotificationGroup>()
            .onEach { updateNotificationGroup(it) }.launchIn(scope)

        client.updateFlow
            .filterIsInstance<TdApi.UpdateNotification>()
            .onEach { updateNotification(it) }.launchIn(scope)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        client.sendUnscopedRequest(
            TdApi.SetOption(
                "notification_group_count_max",
                TdApi.OptionValueInteger(3)
            )
        )
    }

    private fun updateActiveNotifications(update: TdApi.UpdateActiveNotifications) {

        //Log.d(TAG, update.toString())

        update.groups.forEach { group ->
            groups[group.id] = group
        }

        refreshNotifications()

    }

    private fun updateNotificationGroup(update: TdApi.UpdateNotificationGroup) {

        //Log.d(TAG, update.toString())

        groups[update.notificationGroupId]?.apply {
            type = update.type
            chatId = update.chatId
            totalCount = update.totalCount
            notifications = (notifications.toSet() + update.addedNotifications.toSet())
                .filterNot { notification ->
                    update.removedNotificationIds.contains(notification.id)
                }
                .sortedBy { notification -> notification.id }
                .toTypedArray()
            groups[update.notificationGroupId] = this

        } ?: run {
            groups[update.notificationGroupId] = TdApi.NotificationGroup(
                update.notificationGroupId,
                update.type,
                update.chatId,
                update.totalCount,
                update.addedNotifications
            )
        }

        refreshNotifications()

    }

    private fun updateNotification(update: TdApi.UpdateNotification) {

        //Log.d(TAG, update.toString())

        groups[update.notificationGroupId]?.apply {
            val idx =
                notifications.indexOfFirst { notification -> notification.id == update.notification.id }
            if (idx >= 0)
                notifications[idx] = update.notification
        }


        refreshNotifications()

    }

    data class MessageObject(val text: String, val sender: Person, val timestamp: Long)

    private fun buildNotification(group: TdApi.NotificationGroup): Notification {
        val chat = chatProvider.getChat(group.chatId)
        val messages = group.notifications
            .map { it.type }
            .filterIsInstance<TdApi.NotificationTypeNewMessage>()

        val messageNotifications = messages
            .map { it.message }
            .map { message ->
                val text = textSummary(message.content)
                val timestamp = message.date * 1000L
                val sender = when (val senderId = message.senderId) {
                    is MessageSenderUser -> Person.Builder()
                        .setName(
                            client.getUser(senderId.userId)
                                ?.let { it.firstName + " " + it.lastName })
                        .setIcon(
                            // TODO: user photo here?
                            IconCompat.createWithResource(
                                context,
                                R.drawable.baseline_emoji_emotions_24
                            )
                        )
                        .build()
                    else -> Person.Builder().build()
                }
                MessageObject(text, sender, timestamp)
            }
            .map {
                NotificationCompat.MessagingStyle.Message(it.text, it.timestamp, it.sender)
            }

        val markAsReadIntent = Intent().also {
            it.action = "MARK_READ"
            it.putExtra("chatId", group.chatId)
            val msgs = messages.map { msg -> msg.message.id }.toLongArray()
            Log.d(TAG, "putting ${msgs.size} messages")
            it.putExtra("msgIds", msgs)
        }

        val markAsReadAction = NotificationCompat.Action
            .Builder(
                R.drawable.baseline_play_arrow_24,
                "Mark as read",
                PendingIntent.getService(context, 0, markAsReadIntent, 0)
            )
            //.addRemoteInput(RemoteInput.Builder("reply").run {setLabel("Reply")}.build())
            .build()


        val mainIntent = Intent(context, MainActivity::class.java)

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.MessagingStyle(Person.Builder().setName("??").build())
                .setConversationTitle(chat?.title)
                .setGroupConversation(chat?.let {
                    it.type is TdApi.ChatTypePrivate || it.type is TdApi.ChatTypeSecret
                } == false)
                .also { s ->
                    messageNotifications.forEach { s.addMessage(it) }
                })
            .setWhen((group.notifications.firstOrNull()?.date ?: 0) * 1000L)
            .addAction(
                R.drawable.baseline_play_arrow_24,
                "Mark as read",
                PendingIntent.getBroadcast(context, 0, markAsReadIntent, 0)
            )
            //.setContentIntent(PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE))
            //.setAutoCancel(true)
            .setGroup(GROUP_ID)
            .build()

    }

    private fun discardReasonText(reason: TdApi.CallDiscardReason): String =
        when (reason) {
            is TdApi.CallDiscardReasonDeclined -> "Declined"
            is TdApi.CallDiscardReasonMissed -> "Missed"
            is TdApi.CallDiscardReasonDisconnected -> "Disconnected"
            is TdApi.CallDiscardReasonHungUp -> "Hung Up"
            is TdApi.CallDiscardReasonEmpty -> ""
            else -> ""
        }

    private fun textSummary(content: TdApi.MessageContent): String =
        when (content) {
            is TdApi.MessageText -> content.text.text
            is TdApi.MessageAudio -> "Audio"
            is TdApi.MessageCall -> discardReasonText(content.discardReason) + " Call"
            is TdApi.MessageVoiceNote -> "Voice note"
            is TdApi.MessageAnimation -> "GIF"
            is TdApi.MessageAnimatedEmoji -> content.emoji
            is TdApi.MessagePhoto -> "Photo"
            is TdApi.MessageVideo -> "Video"
            is TdApi.MessageVideoNote -> "Video note"
            is TdApi.MessageLocation -> "Location"
            is TdApi.MessageContact -> "Contact"
            is TdApi.MessageDocument -> "Document"
            is TdApi.MessagePoll -> "Poll"
            is TdApi.MessageSticker -> content.sticker.emoji + " Sticker"
            else -> "Unsupported message"
        }

    private fun refreshNotifications() {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Weargram")
            .setContentText("This is a summary")
            .setGroup(GROUP_ID)
            .setGroupSummary(true)
            .build()

        val notifications = groups.values
            .filter { it.totalCount > 0 }.map { Pair(it.id, buildNotification(it)) }

        NotificationManagerCompat.from(context).apply {
            notifications.forEach { notify(it.first, it.second) }
            notify(0, notification)
        }

    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "weargram"
        private const val GROUP_ID = "default"
    }
}