package xyz.tolvanen.weargram.ui.chat

import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import org.drinkless.td.libcore.telegram.TdApi
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import xyz.tolvanen.weargram.R
import xyz.tolvanen.weargram.Screen
import xyz.tolvanen.weargram.ui.util.MapView

@Composable
fun MessageContent(
    message: TdApi.Message,
    viewModel: ChatViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val content = message.content
    Log.d("MessageContent", "top")
    Log.d("MessageContent", "kek")
    when (content) {
        is TdApi.MessageText -> TextMessage(content, modifier)
        is TdApi.MessagePhoto -> PhotoMessage(content, viewModel, modifier)
        is TdApi.MessageAudio -> AudioMessage(content, modifier)
        is TdApi.MessageVideo -> VideoMessage(content, viewModel, navController, modifier)
        is TdApi.MessageSticker -> StickerMessage(content, modifier)
        is TdApi.MessageDocument -> DocumentMessage(content, modifier)
        is TdApi.MessageLocation -> LocationMessage(content, modifier)
        is TdApi.MessageAnimatedEmoji -> AnimatedEmojiMessage(content, modifier)
        is TdApi.MessageAnimation -> AnimationMessage(content, modifier)
        is TdApi.MessageCall -> CallMessage(content, modifier)
        is TdApi.MessagePoll -> PollMessage(content, modifier)
        else -> UnsupportedMessage(content, modifier)
    }

}


@Composable
fun TextMessage(content: TdApi.MessageText, modifier: Modifier = Modifier) {
    Text(
        text = content.text.text,
        modifier = modifier,
        style = MaterialTheme.typography.body2
    )
}

@Composable
fun PhotoMessage(
    content: TdApi.MessagePhoto,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val image = remember { viewModel.fetchPhoto(content) }.collectAsState(initial = null)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        image.value?.also {
            Image(
                //modifier = Modifier.fillMaxSize(),
                bitmap = it,
                contentDescription = null
            )
        }
        content.caption.text.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = it,
                modifier = modifier,
                style = MaterialTheme.typography.body2
            )

        }

    }

}

@Composable
fun AudioMessage(content: TdApi.MessageAudio, modifier: Modifier = Modifier) {
    Text("Audio", modifier = modifier)
}

@Composable
fun VideoMessage(
    content: TdApi.MessageVideo,
    viewModel: ChatViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    //Text("Video", modifier = modifier)
    val path =
        remember(content) { viewModel.fetchFile(content.video.video) }.collectAsState(initial = null)
    //val frame = remember(content) {
    //    path.value?.let {
    //        MediaMetadataRetriever().apply {
    //            setDataSource(it)
    //        }.getFrameAtIndex(0)?.asImageBitmap()
    //    }
    //}

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        path.value?.also {

            // TODO: Fetch the frame in a more intelligent fashion
            MediaMetadataRetriever().apply {
                setDataSource(it)
            }.getFrameAtIndex(0)?.asImageBitmap()?.also { frame ->

                Box {
                    Image(
                        frame,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    CompactButton(
                        onClick = {
                            navController.navigate(Screen.Video.buildRoute(it))
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0x7E000000)),
                        modifier = Modifier.align(Alignment.Center)

                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.baseline_play_arrow_24),
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center)
                        )

                    }

                }

            }

        } ?: CircularProgressIndicator()

        content.caption.text.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = it,
                modifier = modifier,
                style = MaterialTheme.typography.body2
            )

        }

    }

}

@Composable
fun StickerMessage(content: TdApi.MessageSticker, modifier: Modifier = Modifier) {
    Text(content.sticker.emoji + " Sticker", modifier = modifier)
}

@Composable
fun DocumentMessage(content: TdApi.MessageDocument, modifier: Modifier = Modifier) {
    Text("file: " + content.document.fileName, modifier = modifier)
}

@Composable
fun LocationMessage(content: TdApi.MessageLocation, modifier: Modifier = Modifier) {

    val context = LocalContext.current
    MapView(
        onLoad = {
            val position = GeoPoint(content.location.latitude, content.location.longitude)
            val marker = Marker(it)
            marker.position = position
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = ContextCompat.getDrawable(context, R.drawable.baseline_location_on_24)
            it.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            it.overlays.add(marker)
            it.setOnTouchListener { v, _ ->
                v.performClick()
                true
            }
            it.controller.animateTo(position, 15.0, 0)
            it.invalidate()
        }
    )
    Text(
        "Location: lat ${content.location.latitude}, lon ${content.location.longitude}",
        modifier = modifier
    )
}

@Composable
fun AnimatedEmojiMessage(content: TdApi.MessageAnimatedEmoji, modifier: Modifier = Modifier) {
    Text(content.emoji, modifier = modifier)
}

@Composable
fun AnimationMessage(content: TdApi.MessageAnimation, modifier: Modifier = Modifier) {
    Text("Animation", modifier = modifier)
}

@Composable
fun CallMessage(content: TdApi.MessageCall, modifier: Modifier = Modifier) {
    Text("Call", modifier = modifier)
}

@Composable
fun PollMessage(content: TdApi.MessagePoll, modifier: Modifier = Modifier) {
    Text("Poll", modifier = modifier)
}

@Composable
fun UnsupportedMessage(content: TdApi.MessageContent, modifier: Modifier = Modifier) {
    Text("Unsupported message", modifier = modifier)
}