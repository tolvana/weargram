package xyz.tolvanen.weargram

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.client.NotificationProvider
import xyz.tolvanen.weargram.client.TelegramClient
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : Service() {

    private val TAG = this::class.simpleName

    @Inject
    lateinit var client: TelegramClient

    @Inject
    lateinit var notificationProvider: NotificationProvider

    private val binder = NotificationBinder()

    inner class NotificationBinder : Binder() {
        val service: NotificationService = this@NotificationService

    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "binding")
        return binder
    }


    private val markAsReadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val chatId = intent.getLongExtra("chatId", 0)
            val msgIds = intent.getLongArrayExtra("msgIds")
            Log.d(TAG, "trying to read ${msgIds?.size} messages from $chatId")
            if (chatId > 0) {
                client.sendUnscopedRequest(TdApi.ViewMessages(chatId, 0, msgIds, true))
            }
        }

    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(markAsReadReceiver, IntentFilter("MARK_READ"))
    }

    override fun onDestroy() {
        unregisterReceiver(markAsReadReceiver)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                FOREGROUND_NOTIFICATION_CHANNEL_ID,
                "Weargram",
                NotificationManager.IMPORTANCE_MIN
            )
        )

        val notification: Notification =
            Notification.Builder(this, FOREGROUND_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Weargram")
                .setContentText("Weargram is running")
                .setContentIntent(pendingIntent)
                .build()

        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        return START_STICKY

    }

    companion object {
        private const val FOREGROUND_NOTIFICATION_CHANNEL_ID = "weargram_foreground"
        private const val FOREGROUND_NOTIFICATION_ID = 97
    }
}