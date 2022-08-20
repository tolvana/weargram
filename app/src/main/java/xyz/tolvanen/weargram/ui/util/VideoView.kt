package xyz.tolvanen.weargram.ui.util

import android.graphics.Paint.Style
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.video.VideoSize


@Composable
fun VideoView(videoUri: String) {

    // https://medium.com/backyard-programmers/media3-exoplayer-in-jetpack-compose-to-make-snapchat-spotlight-75e384e2ef56

    val context = LocalContext.current

    val view = remember {
        StyledPlayerView(context)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val media = MediaItem.Builder()
                .setUri(Uri.parse(videoUri))
                .build()
            setMediaItem(media)
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(
        AndroidView(
            factory = {
                view.apply {
                    player = exoPlayer
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                }
            },
        )

    ) {
        onDispose { exoPlayer.release() }
    }

}