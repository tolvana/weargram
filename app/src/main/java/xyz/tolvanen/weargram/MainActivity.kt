/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package xyz.tolvanen.weargram

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import xyz.tolvanen.weargram.ui.App

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var notificationService: NotificationService? = null
    private var bound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            notificationService = (binder as NotificationService.NotificationBinder).service
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            notificationService = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceIntent = Intent(applicationContext, NotificationService::class.java)
        applicationContext.startForegroundService(serviceIntent)

        Intent(this, NotificationService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        setContent {
            App()
        }
    }
}
