package xyz.tolvanen.weargram.ui.home

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.Screen

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
                .wrapContentHeight()
        ) {
            items(chats) { chatId ->
                chatData[chatId]?.let { chat ->
                    ChatItem(
                        chat,
                        onClick = { navController.navigate(Screen.Chat.buildRoute(chatId)) }
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

    }
}

val TdApi.Message.shortDescription: String
    get() = when (content.constructor) {
        TdApi.MessageText.CONSTRUCTOR -> (content as TdApi.MessageText).text.text
        else -> "Unsupported message"
    }

@Composable
fun ChatItem(chat: TdApi.Chat, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        backgroundPainter = ColorPainter(MaterialTheme.colors.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = chat.title,
                    maxLines = 1,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = chat.lastMessage?.shortDescription ?: "Empty history",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.caption2
                )
            }

            if (chat.unreadCount > 0) {
                CompactButton(
                    onClick = { /*TODO*/ },
                    modifier = Modifier.weight(1f, false)
                ) {
                    Text(text = (if (chat.unreadCount < 100) chat.unreadCount.toString() else "99+"))
                }

            }

        }


    }
}

sealed class HomeState {
    object Loading : HomeState()
    object Login : HomeState()
    object Ready : HomeState()
}
