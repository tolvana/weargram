package xyz.tolvanen.weargram.ui

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.Screen
import xyz.tolvanen.weargram.client.Authorization
import xyz.tolvanen.weargram.client.Authenticator
import xyz.tolvanen.weargram.client.ChatProvider
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authenticator: Authenticator,
    private val chatProvider: ChatProvider
) : ViewModel() {

    val homeState = mutableStateOf<HomeState>(HomeState.Loading)
    val chatData = MutableLiveData<List<TdApi.Chat>>()

    init {
        authenticator.authorizationState.onEach {
            when (it) {
                Authorization.UNAUTHORIZED -> {
                    homeState.value = HomeState.Loading
                    authenticator.startAuthorization()
                }
                Authorization.AUTHORIZED -> {
                    chatProvider.loadChats()
                    homeState.value = HomeState.Ready
                }
                else -> {
                    if (homeState.value != HomeState.Login) {
                        homeState.value = HomeState.Login
                    }
                }
            }
        }.launchIn(viewModelScope)

        chatProvider.chatFlow
            .onEach { chatData.postValue(it) }
            .launchIn(viewModelScope)
    }

}

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
    val chats by viewModel.chatData.observeAsState()

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
            chats?.also {
                items(it) { chat ->
                    ChatItem(
                        chat,
                        onClick = { navController.navigate(Screen.Chat.buildRoute(chat.id)) }
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
    Card(onClick = onClick) {
        Column() {
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

    }
}

sealed class HomeState {
    object Loading : HomeState()
    object Login : HomeState()
    object Ready : HomeState()
}
