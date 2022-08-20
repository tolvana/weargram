package xyz.tolvanen.weargram.ui

import android.app.RemoteInput
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.R
import xyz.tolvanen.weargram.Screen
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


    fun fetchPhoto(photoMessage: TdApi.MessagePhoto): Flow<ImageBitmap?> {
        val file = photoMessage.photo.sizes.last().photo
        return client.getFilePath(file)
            .map {
                Log.d(TAG, "got filepath $it")
                it?.let {
                    BitmapFactory.decodeFile(it)?.asImageBitmap()
                }
            }
    }

}

@Composable
fun ChatScreen(navController: NavController, chatId: Long, viewModel: ChatViewModel) {

    Log.d("ChatScreen", "recomp")
    Log.d("ChatScreen", "yay")
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
                        navController = navController,
                        chatId = chatId,
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
                        MessageItem(message, viewModel)
                    }
                }

                item {
                    LaunchedEffect(true) {
                        // TODO: make sure this is not looped when end of chat history is reached
                        viewModel.pullMessages()
                    }
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}


@Composable
fun MessageItem(message: TdApi.Message, viewModel: ChatViewModel) {

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart,

        ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.8f),
            onClick = { /*TODO*/ },
            contentPadding = PaddingValues(0.dp),
            backgroundPainter = ColorPainter(
                if (message.isOutgoing) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.surface
            ),
        ) {
            MessageContent(
                message,
                viewModel,
                modifier = Modifier.padding(CardDefaults.ContentPadding)
            )
        }
    }
}

@Composable
fun MessageInput(
    navController: NavController,
    chatId: Long,
    sendMessage: (String) -> Unit = {}
) {

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let { data ->
                val results: Bundle = RemoteInput.getResultsFromIntent(data)
                val activityInput: CharSequence? = results.getCharSequence("input")
                sendMessage(activityInput.toString())
            }
        }

    Row(
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                val remoteInputs: List<RemoteInput> = listOf(
                    RemoteInput.Builder("input")
                        .setLabel("Text message?")
                        .wearableExtender {
                            setEmojisAllowed(true)
                            setInputActionType(EditorInfo.IME_ACTION_DONE)
                        }.build()
                )
                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                launcher.launch(intent)
            }) {
            Image(
                painterResource(id = R.drawable.baseline_message_24),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }

        Button(
            onClick = { navController.navigate(Screen.MessageOptions.buildRoute(chatId)) }
        ) {
            Image(
                painterResource(id = R.drawable.baseline_more_horiz_24),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
    }
}