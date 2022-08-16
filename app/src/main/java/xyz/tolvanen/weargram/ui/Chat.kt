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
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.client.TelegramClient
import xyz.tolvanen.weargram.client.ChatProvider
import xyz.tolvanen.weargram.client.MessageProvider
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val client: TelegramClient,
    private val chatProvider: ChatProvider,
    private val messageProvider: MessageProvider,
) : ViewModel() {

    var chatId: Long = -1
        set(value) {
            field = value
            messageProvider.chatId = value
            chatProvider.getChat(value)?.lastMessage?.id?.also {
                lastMessageId.set(it)
            }
            pullMessages()

            client.updateFlow
                .filterIsInstance<TdApi.UpdateNewMessage>()
                .filter { it.message.chatId == chatId }
                .onEach {
                    messageDataState[it.message.id] = it.message
                    messageIds.add(0, it.message.id)
                    // TODO: update this directly to relevant composables as well?
            }.launchIn(viewModelScope)

            client.updateFlow
                .filterIsInstance<TdApi.UpdateDeleteMessages>()
                .filter { it.chatId == chatId }
                .filter { it.isPermanent }
                .onEach {
                    messageIds.removeAll(it.messageIds.toList())
                    it.messageIds.forEach { id -> messageDataState.remove(id) }
            }.launchIn(viewModelScope)

            client.updateFlow
                .filterIsInstance<TdApi.UpdateMessageContent>()
                .filter { it.chatId == chatId }
                .onEach {
                    messageDataState[it.messageId]?.also { msg ->
                        msg.content = it.newContent
                        messageDataState.remove(it.messageId)
                        messageDataState[it.messageId] = msg
                }
            }.launchIn(viewModelScope)

            client.updateFlow
                .filterIsInstance<TdApi.UpdateMessageSendSucceeded>()
                .filter { it.message.chatId == chatId }
                .onEach {
                    messageDataState[it.message.id] = it.message
                    messageIds[messageIds.indexOf(it.oldMessageId)] = it.message.id
                    messageDataState.remove(it.oldMessageId)
            }.launchIn(viewModelScope)
        }

    val messageDataState = mutableStateMapOf<Long, TdApi.Message>()

    private val oldestMessageId = AtomicLong(0)
    private val lastMessageId = AtomicLong(0)

    private val lastQueriedMessageId = AtomicLong(-1)

    // TODO: make this into a set
    val messageIds = mutableStateListOf<Long>()

    fun pullMessages() {
        if (lastQueriedMessageId.get() != oldestMessageId.get()) {
            val msgId = oldestMessageId.get()
            lastQueriedMessageId.set(msgId)

            val messageSource = messageProvider.getMessages(msgId, limit = 5)
            viewModelScope.launch {
                messageSource.firstOrNull()?.also { messages ->
                    messageDataState.putAll(messages.associateBy { message -> message.id })
                    messageIds.addAll(messages.map { message -> message.id })

                    messageIds.lastOrNull()?.also { id ->
                        if (oldestMessageId.get() != id) {
                            oldestMessageId.set(id)
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

    val messageIds = viewModel.messageIds
    val messages = viewModel.messageDataState

    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    Log.d("ChatScaffold", "has ${messageIds.size} messages")

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
                    items(messageIds.toList(), key = { Log.d("lazyList", it.toString()); it }) { id ->
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


