/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package xyz.tolvanen.weargram

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet.Motion
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import xyz.tolvanen.weargram.ui.App

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        setContent {
            App()
        }
    }
}
