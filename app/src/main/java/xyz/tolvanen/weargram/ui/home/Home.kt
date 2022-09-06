package xyz.tolvanen.weargram.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.Chat
import org.drinkless.td.libcore.telegram.TdApi.ChatTypeBasicGroup
import org.drinkless.td.libcore.telegram.TdApi.ChatTypeSupergroup
import xyz.tolvanen.weargram.R
import xyz.tolvanen.weargram.Screen
import xyz.tolvanen.weargram.ui.util.MessageStatusIcon
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel) {

    val homeState by viewModel.homeState

    when (homeState) {
        HomeState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        HomeState.Login -> {
            navController.navigate(Screen.Login.route) {
                launchSingleTop = true
            }
        }
        HomeState.Ready -> {
            HomeScaffold(navController, viewModel)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScaffold(navController: NavController, viewModel: HomeViewModel) {
    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val chats by viewModel.chatProvider.chatIds.collectAsState()
    val chatData by viewModel.chatProvider.chatData.collectAsState()

    Log.d("HomeScaffold", "chats: " + chats?.size.toString())
    Log.d("HomeScaffold", "chatData: " + chatData.size.toString())

    Scaffold(
        positionIndicator = {
            PositionIndicator(
                scalingLazyListState = listState,
                modifier = Modifier
            )
        },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        listState.animateScrollBy(it.verticalScrollPixels)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable()
                .wrapContentHeight(),
        ) {

            item {
                CompactButton(
                    onClick = { navController.navigate(Screen.MainMenu.route) },
                    modifier = Modifier.padding(6.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    Icon(
                        painterResource(id = R.drawable.baseline_menu_24),
                        contentDescription = null,
                    )

                }
            }
            items(chats) { chatId ->
                chatData[chatId]?.let { chat ->
                    ChatItem(
                        chat,
                        onClick = { navController.navigate(Screen.Chat.buildRoute(chatId)) },
                        viewModel
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

    }
}

@Composable
fun ShortText(text: String, modifier: Modifier = Modifier, color: Color = Color(0xFF888888), user: String? = null) {

    Text(
        text = buildAnnotatedString {
            append(user?.let{ "$it: " } ?: "")

            withStyle(style = SpanStyle(color = color)) {
                append(text)
            }

        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.caption2,
        modifier = modifier.padding(top = 4.dp),
    )
}

@Composable
fun ShortDescription(message: TdApi.Message, chat: Chat, viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val altColor = Color(0xFF4588BE)

    val senderId = message.senderId
    val chatType = chat.type
    val myId = remember(viewModel) { viewModel.client.me.value }

    val user = if ( senderId is TdApi.MessageSenderUser) {
        if (senderId.userId == myId) {
            if (chatType is TdApi.ChatTypePrivate && chatType.userId == myId) null
            else "You"
        } else if (chat.type is ChatTypeSupergroup || chat.type is ChatTypeBasicGroup) {
            viewModel.client.getUser(senderId.userId)?.firstName
        } else null
    } else null

    val username = if (senderId is TdApi.MessageSenderUser) {
        if (senderId.userId == myId) { "You" }
        else viewModel.client.getUser(senderId.userId)?.let {
            it.firstName + " " + it.lastName
        }
    } else null

    when (val content = message.content) {
        is TdApi.MessageText -> ShortText(content.text.text, modifier, user = user)
        is TdApi.MessagePhoto -> ShortText("Photo", modifier, color = altColor, user = user)
        is TdApi.MessageAudio -> ShortText("Audio", modifier, color = altColor, user = user)
        is TdApi.MessageVoiceNote -> ShortText("Voice note", modifier, color = altColor, user = user)
        is TdApi.MessageVideo -> ShortText("Video", modifier, color = altColor, user = user)
        is TdApi.MessageVideoNote -> ShortText("Video note", modifier, color = altColor, user = user)
        is TdApi.MessageCall -> ShortText("Call", modifier, color = altColor, user = user)
        is TdApi.MessageAnimation -> ShortText("GIF", modifier, color = altColor, user = user)
        is TdApi.MessageAnimatedEmoji -> ShortText(content.emoji, modifier, color = altColor, user = user)
        is TdApi.MessageLocation -> ShortText("Location", modifier, color = altColor, user = user)
        is TdApi.MessageContact -> ShortText("Contact", modifier, color = altColor, user = user)
        is TdApi.MessageDocument -> ShortText("Document", modifier, color = altColor, user = user)
        is TdApi.MessagePoll -> ShortText("Poll", modifier, color = altColor, user = user)
        is TdApi.MessageSticker -> ShortText(
            content.sticker.emoji + " Sticker",
            modifier,
            color = altColor, user = user
        )

        is TdApi.MessageBasicGroupChatCreate -> ShortText("$username created the group", color = altColor)
        is TdApi.MessageChatAddMembers -> ShortText("$username added members", color = altColor)
        is TdApi.MessageChatChangePhoto -> ShortText("$username changed group photo", color = altColor)
        is TdApi.MessageChatJoinByLink -> ShortText("Member joined via an invite link", color = altColor)
        is TdApi.MessageChatJoinByRequest -> ShortText("Member joined by request", color = altColor)
        is TdApi.MessageChatSetTheme -> ShortText("Chat theme was set", color = altColor)
        is TdApi.MessageChatSetTtl -> ShortText("Chat TTL was set", color = altColor)
        is TdApi.MessageChatUpgradeFrom -> ShortText("Supergroup was created from group", color = altColor)
        is TdApi.MessageChatUpgradeTo -> ShortText("Supergroup was created from group", color = altColor)
        is TdApi.MessageContactRegistered -> ShortText("$username joined Telegram", color = altColor)
        is TdApi.MessageCustomServiceAction -> ShortText(content.text, color = altColor)
        is TdApi.MessageDice -> ShortText("${content.emoji} Dice", color = altColor, user = user)
        is TdApi.MessageExpiredPhoto -> ShortText("Expired Photo", color = altColor, user = user)
        is TdApi.MessageExpiredVideo -> ShortText("Expired Video", color = altColor, user = user)
        is TdApi.MessageGame -> ShortText("Game", color = altColor)
        is TdApi.MessageGameScore -> ShortText("Game Score", color = altColor)
        is TdApi.MessageInviteVideoChatParticipants -> ShortText("Invite to group call", color = altColor)
        is TdApi.MessageInvoice -> ShortText("Invoice", color = altColor)
        //is TdApi.MessagePassportDataReceived -> ShortText("", color = altColor)
        is TdApi.MessagePassportDataSent -> ShortText("Passport data sent", color = altColor)
        is TdApi.MessagePaymentSuccessful -> ShortText("Payment Successful", color = altColor)
        //is TdApi.MessagePaymentSuccessfulBot -> ShortText("", color = altColor)
        is TdApi.MessagePinMessage -> ShortText("Message was pinned", color = altColor)
        is TdApi.MessageProximityAlertTriggered -> ShortText("Proximity alert", color = altColor)
        is TdApi.MessageScreenshotTaken -> ShortText("Screenshot was taken", color = altColor)
        is TdApi.MessageSupergroupChatCreate -> ShortText("Supergroup was created", color = altColor)
        is TdApi.MessageVenue -> ShortText("Venue", color = altColor)
        is TdApi.MessageVideoChatEnded -> ShortText("Video chat ended", color = altColor)
        is TdApi.MessageVideoChatScheduled -> ShortText("Video chat scheduled", color = altColor)
        is TdApi.MessageVideoChatStarted -> ShortText("Video chat started", color = altColor)
        is TdApi.MessageWebsiteConnected -> ShortText("Website connected", color = altColor)
        else -> ShortText("Unsupported message", modifier, color = altColor, user = user)
    }


}

@Composable
fun ChatItem(chat: TdApi.Chat, onClick: () -> Unit = {}, viewModel: HomeViewModel) {
    Card(
        onClick = onClick,
        backgroundPainter = ColorPainter(MaterialTheme.colors.surface),
    ) {

        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            // Chat name
            Text(
                text = chat.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.weight(1f)
            )

            // Time of last message
            DateTime(chat.lastMessage)

        }
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            // Last message content
            chat.lastMessage?.also {
                ShortDescription(it, chat, viewModel, modifier = Modifier.weight(1f))
            }

            // Status indicators
            Row(
                modifier = Modifier.padding(start = 2.dp)
            ) {
                chat.lastMessage?.also {message ->
                    MessageStatusIcon(message, chat, modifier = Modifier
                        .size(20.dp)
                        .padding(top = 4.dp))
                }

                if (chat.unreadMentionCount > 0) {
                    UnreadDot(text = "@", contentModifier = Modifier.padding(bottom = 2.dp))
                }

                if (chat.unreadCount - chat.unreadMentionCount > 0) {
                    UnreadDot(
                        text = if (chat.unreadCount < 100) chat.unreadCount.toString() else "99+"
                    )
                }
            }

        }
    }
}

@Composable
fun DateTime(message: TdApi.Message?) {
    val locale = LocalContext.current.resources.configuration.locales[0]

    val text = remember(message) {
        message?.date?.let {
            val date = Date(it.toLong() * 1000)
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val lastWeek = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -1) }
            val lastYear = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }

            if (date.after(yesterday.time)) {
                DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
            } else if (date.after(lastWeek.time)) {
                SimpleDateFormat("EEE", locale).format(date)
            } else if (date.after(lastYear.time)) {
                SimpleDateFormat("dd MMM", locale).format(date)
            } else {
                DateFormat.getDateInstance(DateFormat.SHORT).format(date)
            }
        }
    }
    Text(
        text ?: "",
        modifier = Modifier.padding(start = 2.dp),
        style = MaterialTheme.typography.body1
    )
}

@Composable
fun UnreadDot(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    text: String = ""
) {
    Box(
        modifier = modifier
            .wrapContentSize()
            .padding(start = 2.dp, top = 2.dp)
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .background(MaterialTheme.colors.primaryVariant, CircleShape)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption3,
            modifier = contentModifier.align(Alignment.Center)
        )
    }

}

sealed class HomeState {
    object Loading : HomeState()
    object Login : HomeState()
    object Ready : HomeState()
}
