package xyz.tolvanen.weargram.ui

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.client.ChatProvider
import xyz.tolvanen.weargram.client.MessageProvider
import xyz.tolvanen.weargram.client.TelegramClient
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val client: TelegramClient,
    private val chatProvider: ChatProvider,
    val messageProvider: MessageProvider,
) : ViewModel() {

    private val TAG = this::class.simpleName

    fun initialize(chatId: Long) {
        messageProvider.initialize(chatId)
        pullMessages()
    }

    fun pullMessages() {
        messageProvider.pullMessages()
    }

    fun sendMessageAsync(content: TdApi.InputMessageContent): Deferred<TdApi.Message> {
        return messageProvider.sendMessageAsync(0, 0, TdApi.MessageSendOptions(), content)
    }

    fun onStart(chatId: Long) {
        client.sendUnscopedRequest(TdApi.OpenChat(chatId))
    }

    fun onStop(chatId: Long) {
        client.sendUnscopedRequest(TdApi.CloseChat(chatId))
    }

    fun updateVisibleItems(visibleItems: List<ScalingLazyListItemInfo>) {
        messageProvider.updateSeenItems(
            visibleItems.map { it.key }.filterIsInstance<Long>()
        )
    }

}

@Composable
fun ChatScreen(navController: NavController, chatId: Long, viewModel: ChatViewModel) {

    LaunchedEffect(chatId) { viewModel.initialize(chatId) }
    DisposableEffect(viewModel) {
        viewModel.onStart(chatId)
        onDispose { viewModel.onStop(chatId) }
    }

    ChatScaffold(navController, chatId, viewModel)
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun ChatScaffold(navController: NavController, chatId: Long, viewModel: ChatViewModel) {

    val messageIds by viewModel.messageProvider.messageIds.collectAsState()
    val messages by viewModel.messageProvider.messageData.collectAsState()

    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .distinctUntilChanged()
            .collect {
                viewModel.updateVisibleItems(it)
            }
    }

    //BottomSheetScaffold(sheetContent = {}) {
        Scaffold(
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = listState,
                    modifier = Modifier
                )
            },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        ) {
            Column() {

                ScalingLazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .onRotaryScrollEvent {
                            coroutineScope.launch {
                                listState.animateScrollBy(it.verticalScrollPixels)
                            }
                            false
                        }
                        .focusRequester(focusRequester)
                        .focusable()

                ) {
                    item {
                        val input = remember { mutableStateOf("") }
                        val scope = rememberCoroutineScope()
                        MessageInput(
                            input = input,
                            sendMessage = {
                                scope.launch {
                                    viewModel.sendMessageAsync(
                                        content = TdApi.InputMessageText(
                                            TdApi.FormattedText(
                                                it,
                                                emptyArray()
                                            ), false, false
                                        )
                                    ).await()

                                    input.value = ""
                                    // TODO: refresh history

                                }
                            }
                        )
                    }
                    items(messageIds.toList(), key = { it }) { id ->
                        messages[id]?.also { message ->
                            MessageItem(message)
                        }
                    }

                    item {
                        LaunchedEffect(true) {
                            Log.d("ChatScaffold", "end of list reached")
                            viewModel.pullMessages()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

            }

        //}

    }

}
@Composable
fun MessageItem(message: TdApi.Message) {
    if (message.isOutgoing) {

    }
    Card(onClick = { /*TODO*/ }) {
        MessageContent(message)
    }

}

@Composable
fun MessageInput(
    input: MutableState<String> = remember { mutableStateOf("") },
    sendMessage: (String) -> Unit = {}
) {

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let { data ->
                val results: Bundle = RemoteInput.getResultsFromIntent(data)
                val activityInput: CharSequence? = results.getCharSequence("input")
                input.value = activityInput.toString()
            }
        }

    Column() {
        CompactButton(
            onClick = {
                val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                val remoteInputs: List<RemoteInput> = listOf(
                    RemoteInput.Builder("input")
                        .setLabel("write msg")
                        .wearableExtender {
                            setEmojisAllowed(true)
                            setInputActionType(EditorInfo.IME_ACTION_DONE)
                        }.build()
                )

                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)

                launcher.launch(intent)

            }) {
            Text("msg")

        }
        if (input.value != "") {
            Text(text = input.value)
            CompactButton(onClick = { sendMessage(input.value) }) {
                Text(text = "snd")
            }

        }

    }
}


