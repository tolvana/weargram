package xyz.tolvanen.weargram.ui

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.TelegramClient
import xyz.tolvanen.weargram.client.MessageProvider
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val telegramClient: TelegramClient,
    private val messageProvider: MessageProvider
) : ViewModel() {

    var chatId: Long = -1
        set(value) {
            field = value
            messageProvider.chatId = value
            telegramClient.getChat(value)?.lastMessage?.id?.also {
                _latestMessageId.value = it
            }
            pullMessages()

            telegramClient.newMessageFlow.filter { it.chatId == chatId }.onEach {
                Log.d("ChatViewModel", "new message!")
                messageDataState[it.id] = it
                messageIds.add(0, it.id)
                // TODO: update this directly to relevant composables as well?
            }.launchIn(viewModelScope)

            telegramClient.deleteMessagesFlow.filter { it.chatId == chatId }.onEach {
                Log.d("ChatViewModel", "message deleted!")
                messageIds.removeAll(it.messageIds.toList())
                it.messageIds.forEach { id -> messageDataState.remove(id) }
            }.launchIn(viewModelScope)

            telegramClient.messageUpdateFlow.filter { it.chatId == chatId }.onEach {
                Log.d("ChatViewModel", "message update!")
                messageDataState[it.messageId]?.also { msg ->
                    msg.content = it.newContent
                    messageDataState.remove(it.messageId)
                    messageDataState[it.messageId] = msg
                }
                //messageDataState[it.messageId]?.also {message ->
                //    message.content = it.newContent
                //    messageDataState[it.messageId] = message
                //}
            }.launchIn(viewModelScope)

            telegramClient.messageSendSucceededFlow.filter { it.message.chatId == chatId }.onEach {
                Log.d("ChatViewModel", "message sent!")
                messageDataState[it.message.id] = it.message
                messageIds[messageIds.indexOf(it.oldMessageId)] = it.message.id
                messageDataState.remove(it.oldMessageId)
            }.launchIn(viewModelScope)
        }

    val messageDataState = mutableStateMapOf<Long, TdApi.Message>()

    private val _oldestMessageId = mutableStateOf(0L)
    private val _latestMessageId = mutableStateOf(-1L)
    val oldestMessageId: State<Long> get() = _oldestMessageId
    val latestMessageId: State<Long> get() = _latestMessageId

    private val lastQueriedMessageId = mutableStateOf(-1L)

    // TODO: make this into a set
    val messageIds = mutableStateListOf<Long>()

    fun pullMessages() {
        if (lastQueriedMessageId.value != _oldestMessageId.value) {
            val msgId = _oldestMessageId.value
            lastQueriedMessageId.value = msgId

            Log.d("ChatViewModel", "pullMessages called with msgId $msgId")
            val messageSource = messageProvider.getMessages(msgId, limit = 5)
            viewModelScope.launch {
                messageSource.firstOrNull()?.also { messages ->
                    messageDataState.putAll(messages.associateBy { message -> message.id })
                    messageIds.addAll(messages.map { message -> message.id })
                    Log.d("ChatViewModel", "got ${messages.size} new messages")
                    Log.d("ChatViewModel", messages.joinToString { it.id.toString() })

                    messageIds.lastOrNull()?.also { id ->
                        if (_oldestMessageId.value != id) {
                            _oldestMessageId.value = id
                        }
                    }
                }
            }
        }
    }

    fun sendMessageAsync(content: TdApi.InputMessageContent): Deferred<TdApi.Message> {
        return messageProvider.sendMessageAsync(0, 0, TdApi.MessageSendOptions(), content)
    }

}

@Composable
fun ChatScreen(navController: NavController, chatId: Long, viewModel: ChatViewModel) {

    LaunchedEffect(chatId) {
        viewModel.chatId = chatId
    }

    ChatScaffold(navController, chatId, viewModel)
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun ChatScaffold(navController: NavController, chatId: Long, viewModel: ChatViewModel) {

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let { data ->
                val results: Bundle = RemoteInput.getResultsFromIntent(data)
                val input: CharSequence? = results.getCharSequence("input")
                Log.d("ChatScaffold", "send msg ${input.toString()}")
                //onEntry(input.toString())
            }
        }

    val latestMessageId by viewModel.latestMessageId
    val oldestMessageId by viewModel.oldestMessageId
    val messageIds = viewModel.messageIds
    val messages = viewModel.messageDataState

    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    Log.d("ChatScaffold", "has ${messageIds?.size} messages")

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
                            if (message.content.constructor == TdApi.MessageText.CONSTRUCTOR) {
                                Card(onClick = { /*TODO*/ }) {
                                    Text(text = (message.content as TdApi.MessageText).text.text)
                                }
                            }
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


