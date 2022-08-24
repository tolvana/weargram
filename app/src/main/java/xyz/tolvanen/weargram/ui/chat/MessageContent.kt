package xyz.tolvanen.weargram.ui.chat

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
import xyz.tolvanen.weargram.ui.util.VideoView
import java.text.DateFormat

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
        is TdApi.MessageText -> TextMessage(message, content, viewModel, modifier)
        is TdApi.MessagePhoto -> PhotoMessage(message, content, viewModel, modifier)
        is TdApi.MessageAudio -> AudioMessage(message, content, viewModel, modifier)
        is TdApi.MessageVideo -> VideoMessage(message, content, viewModel, navController, modifier)
        is TdApi.MessageSticker -> StickerMessage(message, content, viewModel, modifier)
        is TdApi.MessageDocument -> DocumentMessage(message, content, viewModel, modifier)
        is TdApi.MessageLocation -> LocationMessage(message, content, viewModel, modifier)
        is TdApi.MessageAnimatedEmoji -> AnimatedEmojiMessage(message, content, viewModel, modifier)
        is TdApi.MessageAnimation -> AnimationMessage(message, content, viewModel, modifier)
        is TdApi.MessageCall -> CallMessage(message, content, viewModel, modifier)
        is TdApi.MessagePoll -> PollMessage(message, content, viewModel, modifier)
        else -> UnsupportedMessage(message, modifier)
    }
}

@Composable
fun MessageCard(
    message: TdApi.Message,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit)
) {
    Card(
        onClick = { Log.d("Card", "was clicked") },
        contentPadding = PaddingValues(0.dp),
        backgroundPainter = ColorPainter(
            if (message.isOutgoing) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.surface
        ),
    ) { content() }
}


@Composable
fun TextMessage(
    message: TdApi.Message,
    content: TdApi.MessageText,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    MessageCard(message) {

        Column(
            modifier = modifier.padding(CardDefaults.ContentPadding),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = content.text.text, style = MaterialTheme.typography.body2
            )

            MessageInfo(message, viewModel)

        }
    }
}

@Composable
fun MessageInfo(message: TdApi.Message, viewModel: ChatViewModel) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Time(message.date)
        if (message.isOutgoing) {
            Status(message, viewModel)
        }

    }
}

@Composable
fun Time(timestamp: Int) {
    Text(
        text = DateFormat.getTimeInstance(DateFormat.SHORT).format(timestamp.toLong() * 1000),
        modifier = Modifier.padding(0.dp),
        style = MaterialTheme.typography.caption1
    )
}

@Composable
fun Status(message: TdApi.Message, viewModel: ChatViewModel) {

    val chat = viewModel.chatFlow.collectAsState()

    val iconModifier = Modifier
        .size(20.dp)
        .padding(start = 2.dp)
    when (message.sendingState) {
        is TdApi.MessageSendingStatePending -> {
            Icon(
                imageVector = Icons.Outlined.Pending,
                contentDescription = null,
                iconModifier
            )
        }
        is TdApi.MessageSendingStateFailed -> {
            Icon(
                imageVector = Icons.Outlined.SyncProblem,
                contentDescription = null,
                iconModifier
            )
        }
        else -> {
            val lastReadId = chat.value.lastReadOutboxMessageId
            if ((message.interactionInfo?.viewCount ?: 0) > 0 || lastReadId >= message.id) {
                Icon(
                    imageVector = Icons.Outlined.DoneAll,
                    contentDescription = null,
                    iconModifier
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Done,
                    contentDescription = null,
                    iconModifier
                )

            }
        }
    }

}

@Composable
fun PhotoMessage(
    message: TdApi.Message,
    content: TdApi.MessagePhoto,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val image = remember { viewModel.fetchPhoto(content) }.collectAsState(initial = null)

    MessageCard(message) {
        Column(
            modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top
        ) {
            image.value?.also {
                Image(
                    //modifier = Modifier.fillMaxSize(),
                    bitmap = it, contentDescription = null
                )
            }
            content.caption.text.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it, style = MaterialTheme.typography.body2,
                    modifier = modifier.padding(CardDefaults.ContentPadding),
                )

            }

        }
    }

}

@Composable
fun AudioMessage(
    message: TdApi.Message,
    content: TdApi.MessageAudio,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {

    MessageCard(message) {
        Text(
            "Audio",
            modifier = modifier.padding(CardDefaults.ContentPadding),
        )
    }
}

@Composable
fun VideoMessage(
    message: TdApi.Message,
    content: TdApi.MessageVideo,
    viewModel: ChatViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val path =
        remember(content) { viewModel.fetchFile(content.video.video) }.collectAsState(initial = null)

    MessageCard(message) {
        Column(
            modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top
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

            } ?: CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(4.dp)
            )

            content.caption.text.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it, style = MaterialTheme.typography.body2,
                    modifier = modifier.padding(CardDefaults.ContentPadding),
                )

            }

        }
    }

}

@Composable
fun StickerMessage(
    message: TdApi.Message,
    content: TdApi.MessageSticker,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val path =
        remember { viewModel.fetchFile(content.sticker.sticker) }.collectAsState(initial = null)
    path.value?.also {
        BitmapFactory.decodeFile(it)?.asImageBitmap()?.also { bitmap ->
            Image(
                bitmap = bitmap, contentDescription = null
            )
        } ?: MessageCard(message) {
            Text(
                content.sticker.emoji + " Sticker",
                modifier = modifier.padding(CardDefaults.ContentPadding),
            )
        }
    }
}

@Composable
fun DocumentMessage(
    message: TdApi.Message,
    content: TdApi.MessageDocument,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    MessageCard(message) {
        Text(
            "file: " + content.document.fileName,
            modifier = modifier.padding(CardDefaults.ContentPadding),
        )
    }
}

@Composable
fun LocationMessage(
    message: TdApi.Message,
    content: TdApi.MessageLocation, viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    MessageCard(message) {
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
            },
            modifier = Modifier
                .defaultMinSize(minHeight = 120.dp)
                .fillMaxSize()
        )
    }
}

@Composable
fun AnimatedEmojiMessage(
    message: TdApi.Message,
    content: TdApi.MessageAnimatedEmoji,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    MessageCard(message) {
        Text(
            content.emoji,
            modifier = modifier.padding(CardDefaults.ContentPadding),
        )
    }
}

@Composable
fun AnimationMessage(
    message: TdApi.Message,
    content: TdApi.MessageAnimation,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val path =
        remember { viewModel.fetchFile(content.animation.animation) }.collectAsState(initial = null)

    MessageCard(message) {
        path.value?.also {
            VideoView(videoUri = it, repeat = true)
        } ?: run { CircularProgressIndicator(modifier = modifier.padding(4.dp)) }
    }
}

@Composable
fun CallMessage(
    message: TdApi.Message,
    content: TdApi.MessageCall,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    MessageCard(message) {
        Text(
            "Call",
            modifier = modifier.padding(CardDefaults.ContentPadding),
        )
    }
}

@Composable
fun PollMessage(
    message: TdApi.Message,
    content: TdApi.MessagePoll,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    MessageCard(message) {
        Text(
            "Poll",
            modifier = modifier.padding(CardDefaults.ContentPadding),
        )
    }
}

@Composable
fun UnsupportedMessage(message: TdApi.Message, modifier: Modifier = Modifier) {
    MessageCard(message) {
        Text(
            "Unsupported message",
            modifier = modifier.padding(CardDefaults.ContentPadding),
        )
    }
}