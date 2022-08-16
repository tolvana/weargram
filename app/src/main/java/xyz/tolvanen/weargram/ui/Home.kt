package xyz.tolvanen.weargram.ui

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
import xyz.tolvanen.weargram.Authorization
import xyz.tolvanen.weargram.Screen
import xyz.tolvanen.weargram.TelegramClient
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val telegramClient: TelegramClient
) : ViewModel() {

    val homeState = mutableStateOf<HomeState>(HomeState.Loading)
    val chatState = mutableStateOf<List<TdApi.Chat>>(listOf())
    val chatData = MutableLiveData<List<TdApi.Chat>>()


    init {
        telegramClient.authorizationState.onEach {
            when (it) {
                Authorization.UNAUTHORIZED -> {
                    homeState.value = HomeState.Loading
                    telegramClient.startAuthorization()
                }
                Authorization.AUTHORIZED -> {
                    homeState.value = HomeState.Ready
                }
                else -> {
                    if (homeState.value != HomeState.Login) {
                        homeState.value = HomeState.Login
                    }
                }
            }
        }.launchIn(viewModelScope)

        telegramClient.chatFlow.onEach {
            chatState.value = it
            chatData.postValue(it)
            //Log.d("top", "chats: ${it.joinToString { chat -> chat.title }}")
        }.launchIn(viewModelScope)

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
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom)}
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
                items(it) {chat ->
                    Chip(
                        modifier = Modifier.wrapContentSize(Alignment.Center),
                        label = { Text(chat.title) },
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

sealed class HomeState {
    object Loading : HomeState()
    object Login : HomeState()
    object Ready : HomeState()
}
