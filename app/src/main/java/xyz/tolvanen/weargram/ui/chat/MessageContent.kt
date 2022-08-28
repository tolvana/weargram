package xyz.tolvanen.weargram.ui.chat

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.flow.distinctUntilChanged
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
    sender: String? = null,
    modifier: Modifier = Modifier
) {

    when (val content = message.content) {
        is TdApi.MessageText -> TextMessage(message, content, viewModel, modifier, sender = sender)
        is TdApi.MessagePhoto -> PhotoMessage(
            message, content, viewModel, modifier, sender = sender
        )
        is TdApi.MessageAudio -> AudioMessage(
            message, content, viewModel, modifier, sender = sender
        )

        is TdApi.MessageVoiceNote -> VoiceNoteMessage(
            message, content, viewModel, modifier, sender = sender
        )
        is TdApi.MessageVideo -> VideoMessage(
            message, content, viewModel, navController, modifier, sender = sender
        )
        is TdApi.MessageSticker -> StickerMessage(
            message, content, viewModel, modifier, sender = sender
        )
        is TdApi.MessageDocument -> DocumentMessage(
            message, content, viewModel, modifier, sender = sender
        )
        is TdApi.MessageLocation -> LocationMessage(
            message, content, viewModel, modifier, sender = sender
        )
        is TdApi.MessageAnimatedEmoji -> AnimatedEmojiMessage(
            message, content, viewModel, modifier, sender = sender
        )
        is TdApi.MessageAnimation -> AnimationMessage(
            message, content, viewModel, modifier, sender = sender
        )
        is TdApi.MessageCall -> CallMessage(message, content, viewModel, modifier, sender = sender)
        is TdApi.MessagePoll -> PollMessage(message, content, viewModel, modifier, sender = sender)
        else -> UnsupportedMessage(message, modifier, sender = sender)
    }
}

@Composable
fun MessageCard(
    message: TdApi.Message,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    content: @Composable (ColumnScope.() -> Unit)
) {
    Card(
        onClick = { Log.d("Card", "was clicked") },
        contentPadding = contentPadding,
        backgroundPainter = ColorPainter(
            if (message.isOutgoing) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.surface
        ),
    ) { content() }
}

@Composable
fun MessageInfo(message: TdApi.Message, viewModel: ChatViewModel) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (message.editDate > message.date)
            Text(
                "edited",
                style = MaterialTheme.typography.caption1,
                modifier = Modifier.padding(end = 2.dp)
            )
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
            Icon(imageVector = Icons.Outlined.Pending, contentDescription = null, iconModifier)
        }
        is TdApi.MessageSendingStateFailed -> {
            Icon(imageVector = Icons.Outlined.SyncProblem, contentDescription = null, iconModifier)
        }
        else -> {
            val lastReadId = chat.value.lastReadOutboxMessageId
            if ((message.interactionInfo?.viewCount ?: 0) > 0 || lastReadId >= message.id) {
                Icon(imageVector = Icons.Outlined.DoneAll, contentDescription = null, iconModifier)
            } else {
                Icon(imageVector = Icons.Outlined.Done, contentDescription = null, iconModifier)
            }
        }
    }
}

@Composable
fun FormattedText(text: TdApi.FormattedText, modifier: Modifier = Modifier) {

    val formattedString = buildAnnotatedString {
        pushStyle(SpanStyle(color = MaterialTheme.colors.onSurfaceVariant))
        append(text.text)
        for (entity in text.entities) {
            when (val entityType = entity.type) {
                is TdApi.TextEntityTypeBold -> {
                    addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        entity.offset,
                        entity.offset + entity.length
                    )
                }
                is TdApi.TextEntityTypeItalic -> {
                    addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        entity.offset,
                        entity.offset + entity.length
                    )
                }
                is TdApi.TextEntityTypeCode -> {
                    addStyle(
                        SpanStyle(fontFamily = FontFamily.Monospace),
                        entity.offset,
                        entity.offset + entity.length
                    )
                }
                is TdApi.TextEntityTypeUnderline -> {
                    addStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        entity.offset,
                        entity.offset + entity.length
                    )
                }
                is TdApi.TextEntityTypeStrikethrough -> {
                    addStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough),
                        entity.offset,
                        entity.offset + entity.length
                    )
                }
                is TdApi.TextEntityTypeTextUrl -> {
                    addStyle(
                        style = SpanStyle(
                            color = Color(0xff64B5F6), textDecoration = TextDecoration.Underline
                        ), start = entity.offset, end = entity.offset + entity.length
                    )

                    addStringAnnotation(
                        tag = "url",
                        annotation = entityType.url,
                        start = entity.offset,
                        end = entity.offset + entity.length
                    )
                }
                is TdApi.TextEntityTypeUrl -> {
                    addStyle(
                        style = SpanStyle(
                            color = Color(0xff64B5F6), textDecoration = TextDecoration.Underline
                        ), start = entity.offset, end = entity.offset + entity.length
                    )

                    addStringAnnotation(
                        tag = "url",
                        annotation = text.text.drop(entity.offset).take(entity.length),
                        start = entity.offset,
                        end = entity.offset + entity.length
                    )
                }
                is TdApi.TextEntityTypeEmailAddress -> {
                    addStyle(
                        style = SpanStyle(
                            color = Color(0xff64B5F6), textDecoration = TextDecoration.Underline
                        ), start = entity.offset, end = entity.offset + entity.length
                    )
                    addStringAnnotation(
                        tag = "email",
                        annotation = text.text.drop(entity.offset).take(entity.length),
                        start = entity.offset,
                        end = entity.offset + entity.length
                    )
                }
                is TdApi.TextEntityTypePhoneNumber -> {
                    addStyle(
                        style = SpanStyle(
                            color = Color(0xff64B5F6), textDecoration = TextDecoration.Underline
                        ), start = entity.offset, end = entity.offset + entity.length
                    )
                    addStringAnnotation(
                        tag = "phone",
                        annotation = text.text.drop(entity.offset).take(entity.length),
                        start = entity.offset,
                        end = entity.offset + entity.length
                    )
                }
                is TdApi.TextEntityTypeBankCardNumber -> {}
                is TdApi.TextEntityTypeBotCommand -> {}
                is TdApi.TextEntityTypeCashtag -> {}
                is TdApi.TextEntityTypeHashtag -> {}
                is TdApi.TextEntityTypeMediaTimestamp -> {}
                is TdApi.TextEntityTypeMention -> {}
                is TdApi.TextEntityTypeMentionName -> {}
                is TdApi.TextEntityTypePre -> {}
                is TdApi.TextEntityTypePreCode -> {}
            }
        }
    }

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    ClickableText(
        text = formattedString, onClick = {
            formattedString.getStringAnnotations("url", it, it).firstOrNull()
                ?.let { stringAnnotation ->
                    uriHandler.openUri(stringAnnotation.item)
                }

            formattedString.getStringAnnotations("email", it, it).firstOrNull()
                ?.let { stringAnnotation ->
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, stringAnnotation.item)
                    }
                    emailIntent.resolveActivity(context.packageManager)?.also {
                        startActivity(context, emailIntent, null)
                    }
                }

            formattedString.getStringAnnotations("phone", it, it).firstOrNull()
                ?.let { stringAnnotation ->
                    val phoneIntent = Intent(
                        Intent.ACTION_DIAL, Uri.fromParts("tel", stringAnnotation.item, null)
                    )
                    phoneIntent.resolveActivity(context.packageManager)?.also {
                        startActivity(context, phoneIntent, null)
                    }
                }
        }, style = MaterialTheme.typography.body2, modifier = modifier
    )
}

@Composable
fun Sender(sender: String?) {
    sender?.also {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 5.dp, bottom = 2.dp)
        ) {
            Text(sender)
        }
    }
}


@Composable
fun TextMessage(
    message: TdApi.Message,
    content: TdApi.MessageText,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    sender: String?
) {

    MessageCard(message) {

        Column(
            horizontalAlignment = Alignment.Start
        ) {
            sender?.also { Text(text = it) }
            FormattedText(content.text)
            MessageInfo(message, viewModel)
        }
    }
}


@Composable
fun PhotoMessage(
    message: TdApi.Message,
    content: TdApi.MessagePhoto,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    sender: String?
) {
    val image = remember { viewModel.fetchPhoto(content) }.collectAsState(initial = null)

    MessageCard(message, contentPadding = PaddingValues(0.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {

            Sender(sender)

            image.value?.also { Image(bitmap = it, contentDescription = null) }

            content.caption.text.takeIf { it.isNotEmpty() }?.let {
                FormattedText(
                    text = content.caption, modifier = modifier.padding(CardDefaults.ContentPadding)
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
    modifier: Modifier = Modifier,
    sender: String?
) {
    // TODO: think about audio playback lifecycle
    val player =
        remember { viewModel.fetchAudio(content.audio.audio) }.collectAsState(initial = null)

    val isPlaying = remember { mutableStateOf(false) }
    val position = remember { mutableStateOf(0f) }


    MessageCard(message) {
        sender?.also { Text(text = it) }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CompactButton(
                onClick = {
                    player.value?.also { p ->

                        if (isPlaying.value) {
                            p.pause()
                        }
                        else {
                            p.start()
                            object : CountDownTimer((p.duration - p.currentPosition).toLong(), 50) {
                                override fun onTick(p0: Long) {
                                    if (isPlaying.value) {
                                        position.value = p.currentPosition.toFloat() / p.duration
                                    }
                                }

                                override fun onFinish() {}
                            }.start()

                            p.setOnCompletionListener {
                                position.value = 0f
                                isPlaying.value = false
                            }
                        }

                        isPlaying.value = !isPlaying.value
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
            ) {
                Icon(
                    painter = painterResource(
                        id =
                        if (isPlaying.value) R.drawable.baseline_pause_circle_24 else R.drawable.baseline_play_circle_24
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface,
                    modifier = Modifier.fillMaxSize()
                )

            }
            CircularProgressIndicator(progress = position.value)
        }

        content.caption.text.takeIf { it.isNotEmpty() }?.let {
            Text(text = it, style = MaterialTheme.typography.body2)
        }

        MessageInfo(message, viewModel)

    }

}

@Composable
fun VoiceNoteMessage(
    message: TdApi.Message,
    content: TdApi.MessageVoiceNote,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    sender: String?
) {
    // TODO: think about audio playback lifecycle
    val player =
        remember { viewModel.fetchAudio(content.voiceNote.voice) }.collectAsState(initial = null)

    val isPlaying = remember { mutableStateOf(false) }
    val position = remember { mutableStateOf(0f) }


    MessageCard(message) {
        sender?.also { Text(text = it) }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CompactButton(
                onClick = {
                    player.value?.also { p ->

                        if (isPlaying.value) {
                            p.pause()
                        }
                        else {
                            p.start()
                            object : CountDownTimer((p.duration - p.currentPosition).toLong(), 50) {
                                override fun onTick(p0: Long) {
                                    if (isPlaying.value) {
                                        position.value = p.currentPosition.toFloat() / p.duration
                                    }
                                }

                                override fun onFinish() {}
                            }.start()

                            p.setOnCompletionListener {
                                position.value = 0f
                                isPlaying.value = false
                            }
                        }

                        isPlaying.value = !isPlaying.value
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
            ) {
                Icon(
                    painter = painterResource(
                        id =
                        if (isPlaying.value) R.drawable.baseline_pause_circle_24 else R.drawable.baseline_play_circle_24
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface,
                    modifier = Modifier.fillMaxSize()
                )

            }
            CircularProgressIndicator(progress = position.value)
        }

        content.caption.text.takeIf { it.isNotEmpty() }?.let {
            Text(text = it, style = MaterialTheme.typography.body2)
        }

        MessageInfo(message, viewModel)

    }

}

@Composable
fun VideoMessage(
    message: TdApi.Message,
    content: TdApi.MessageVideo,
    viewModel: ChatViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    sender: String?
) {
    val path =
        remember(content) { viewModel.fetchFile(content.video.video) }.collectAsState(initial = null)

    MessageCard(message, contentPadding = PaddingValues(0.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top
        ) {

            Sender(sender)

            path.value?.also {

                // TODO: Fetch the frame in a more intelligent fashion
                // SUS! Often causes crashes. Maybe just show thumbnail instead
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
                            }, colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(
                                    0x7E000000
                                )
                            ), modifier = Modifier.align(Alignment.Center)

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
    modifier: Modifier = Modifier,
    sender: String?
) {
    val path =
        remember { viewModel.fetchFile(content.sticker.sticker) }.collectAsState(initial = null)

    path.value?.also {
        BitmapFactory.decodeFile(it)?.asImageBitmap()?.also { bitmap ->
            Column {
                Sender(sender)
                Image(bitmap = bitmap, contentDescription = null)
            }
        } ?: MessageCard(message) {
            sender?.also { s -> Text(text = s) }
            Text(content.sticker.emoji + " Sticker")
        }
    }
}

@Composable
fun DocumentMessage(
    message: TdApi.Message,
    content: TdApi.MessageDocument,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    sender: String?
) {
    MessageCard(message) {
        sender?.also { Text(text = it) }
        Text("file: " + content.document.fileName)
    }
}

@Composable
fun LocationMessage(
    message: TdApi.Message,
    content: TdApi.MessageLocation,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    sender: String?
) {

    val context = LocalContext.current
    MessageCard(message, contentPadding = PaddingValues(0.dp)) {

        Sender(sender)

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
            }, modifier = Modifier
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
    modifier: Modifier = Modifier,
    sender: String?
) {
    MessageCard(message) {
        sender?.also { Text(text = it) }
        Text(content.emoji)
    }
}

@Composable
fun AnimationMessage(
    message: TdApi.Message,
    content: TdApi.MessageAnimation,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    sender: String?
) {
    val path =
        remember { viewModel.fetchFile(content.animation.animation) }.collectAsState(initial = null)

    MessageCard(message, contentPadding = PaddingValues(0.dp)) {

        Sender(sender)

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
    modifier: Modifier = Modifier,
    sender: String?
) {
    MessageCard(message) {
        sender?.also { Text(text = it) }
        Text("Call")
    }
}

@Composable
fun PollMessage(
    message: TdApi.Message,
    content: TdApi.MessagePoll,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    sender: String?
) {
    MessageCard(message) {
        sender?.also { Text(text = it) }
        Text("Poll")
    }
}

@Composable
fun UnsupportedMessage(message: TdApi.Message, modifier: Modifier = Modifier, sender: String?) {
    MessageCard(message) {
        sender?.also { Text(text = it) }
        Text("Unsupported message")
    }
}