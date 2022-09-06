package xyz.tolvanen.weargram.ui.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import xyz.tolvanen.weargram.client.Authenticator
import xyz.tolvanen.weargram.client.Authorization
import xyz.tolvanen.weargram.client.ChatProvider
import xyz.tolvanen.weargram.client.TelegramClient
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authenticator: Authenticator,
    val client: TelegramClient,
    val chatProvider: ChatProvider
) : ViewModel() {

    val homeState = mutableStateOf<HomeState>(HomeState.Loading)

    init {
        authenticator.authorizationState.onEach {
            Log.d("HomeViewModel", "$it")
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
    }
}

