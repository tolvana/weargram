package xyz.tolvanen.weargram.client

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject


class TelegramClient @Inject constructor(private val parameters: TdApi.TdlibParameters) {

    private val _updateFlow = MutableSharedFlow<TdApi.Update>()
    val updateFlow: SharedFlow<TdApi.Update> get() = _updateFlow

    val users = ConcurrentHashMap<Long, TdApi.User>()
    val basicGroups = ConcurrentHashMap<Long, TdApi.BasicGroup>()
    val supergroups = ConcurrentHashMap<Long, TdApi.Supergroup>()

    val userInfos = ConcurrentHashMap<Long, TdApi.UserFullInfo>()
    val basicGroupInfos = ConcurrentHashMap<Long, TdApi.BasicGroupFullInfo>()
    val supergroupInfos = ConcurrentHashMap<Long, TdApi.SupergroupFullInfo>()

    private val TAG = "TelegramClient"

    private val resultHandler = Client.ResultHandler {
        if (it is TdApi.Update)
            mainScope.launch { _updateFlow.emit(it) }

        when (it) {
            is TdApi.UpdateUser -> {
                users[it.user.id] = it.user
            }
            is TdApi.UpdateUserStatus -> {
                users[it.userId]?.status = it.status
            }
            is TdApi.UpdateBasicGroup -> {
                basicGroups[it.basicGroup.id] = it.basicGroup
            }
            is TdApi.UpdateSupergroup -> {
                supergroups[it.supergroup.id] = it.supergroup
            }
            is TdApi.UpdateUserFullInfo -> {
                userInfos[it.userId] = it.userFullInfo
            }
            is TdApi.UpdateBasicGroupFullInfo -> {
                basicGroupInfos[it.basicGroupId] = it.basicGroupFullInfo
            }
            is TdApi.UpdateSupergroupFullInfo -> {
                supergroupInfos[it.supergroupId] = it.supergroupFullInfo
            }
        }
    }

    private val client = Client.create(resultHandler, null, null)

    init {
        client.send(TdApi.SetLogVerbosityLevel(0), resultHandler)

    }

    private val requestScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun sendUnscopedRequest(request: TdApi.Function) {
            client.send(request) {}
        }

    fun sendRequest(request: TdApi.Function): Flow<TdApi.Object> =
        callbackFlow {
            requestScope.launch {
                client.send(request) {
                    trySend(it)
                }
            }
            awaitClose {}
        }

    fun start() {
        requestScope.launch { client.send(TdApi.SetTdlibParameters(parameters)) {} }
    }

    fun getFilePath(file: TdApi.File): Flow<String?> =
        file.takeIf {
            it.local?.isDownloadingCompleted == false
        }?.let {
            downloadFileAsync(it)
        } ?: flowOf(file.local?.path)

    private fun downloadFileAsync(file: TdApi.File): Flow<String?> = callbackFlow {
        requestScope.launch {
            sendRequest(TdApi.DownloadFile(file.id, 1, 0, 0, true)).collect {
                if (it is TdApi.File) {
                    trySend(it.local?.path)
                } else {
                    trySend(null)
                }
            }
        }
        awaitClose {  }

    }

}

