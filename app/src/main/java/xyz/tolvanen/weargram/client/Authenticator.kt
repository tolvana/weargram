package xyz.tolvanen.weargram.client

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import javax.inject.Inject

class Authenticator @Inject constructor(private val client: TelegramClient) {

    private val TAG = this::class.simpleName

    private val _authorizationState = MutableStateFlow(Authorization.UNAUTHORIZED)
    val authorizationState: StateFlow<Authorization> get() = _authorizationState

    private val _tokenState = MutableStateFlow<String?>(null)
    val tokenState: StateFlow<String?> get() = _tokenState

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        client.updateFlow
            .filterIsInstance<TdApi.UpdateAuthorizationState>()
            .onEach { onAuthorizationState(it) }
            .launchIn(scope)

        client.sendUnscopedRequest(TdApi.GetAuthorizationState())
    }

    fun startAuthorization() {
        client.start()
    }

    fun setPhoneNumber(phoneNumber: String) {
        // TODO: sanitize input?
        Log.d(TAG, "phoneNumber: $phoneNumber")
        val settings = TdApi.PhoneNumberAuthenticationSettings()

        scope.launch {
            client.sendRequest(TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings)).collect {
                Log.d(TAG, "phoneNumber. result: $it")
                when (it.constructor) {
                    TdApi.Ok.CONSTRUCTOR -> {
                        Log.d(TAG, "phone number ok")
                    }
                    TdApi.Error.CONSTRUCTOR -> {
                        Log.d(TAG, "phone number error")
                        _authorizationState.value = Authorization.INVALID_NUMBER
                    }
                }

            }

        }
    }

    fun setCode(code: String) {
        // TODO: sanitize input?
        Log.d(TAG, "code: $code")
        scope.launch {
            client.sendRequest(TdApi.CheckAuthenticationCode(code)).collect {
                when (it.constructor) {
                    TdApi.Ok.CONSTRUCTOR -> {
                        Log.d(TAG, "code ok")
                    }
                    TdApi.Error.CONSTRUCTOR -> {
                        Log.d(TAG, "code error")
                        _authorizationState.value = Authorization.INVALID_CODE
                    }
                }
            }
        }
    }

    fun setPassword(password: String) {
        Log.d(TAG, "password: $password")
        scope.launch {
            client.sendRequest(TdApi.CheckAuthenticationPassword(password)).collect {
                when (it.constructor) {
                    TdApi.Ok.CONSTRUCTOR -> {
                        Log.d(TAG, "password ok")
                    }
                    TdApi.Error.CONSTRUCTOR -> {
                        Log.d(TAG, "password error")
                        _authorizationState.value = Authorization.INVALID_PASSWORD
                    }
                }
            }
        }
    }

    private fun onAuthorizationState(authorizationUpdate: TdApi.UpdateAuthorizationState) {
        Log.d(TAG, "here: $authorizationUpdate")

        when (authorizationUpdate.authorizationState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                Log.d(
                    TAG,
                    "onResult: AuthorizationStateWaitTdlibParameters -> state = UNAUTHENTICATED"
                )
                _authorizationState.value = Authorization.UNAUTHORIZED
            }
            is TdApi.AuthorizationStateWaitEncryptionKey -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitEncryptionKey")
                client.sendUnscopedRequest(TdApi.CheckDatabaseEncryptionKey())

            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitPhoneNumber -> state = WAIT_NUMBER")
                client.sendUnscopedRequest(TdApi.RequestQrCodeAuthentication())
                _authorizationState.value = Authorization.WAIT_NUMBER
            }
            is TdApi.AuthorizationStateWaitCode -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitCode -> state = WAIT_CODE")
                _authorizationState.value = Authorization.WAIT_CODE
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitPassword")
                _authorizationState.value = Authorization.WAIT_PASSWORD
            }
            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitOtherDeviceConfirmation")
                _authorizationState.value = Authorization.WAIT_NUMBER
                val link =
                    (authorizationUpdate.authorizationState as TdApi.AuthorizationStateWaitOtherDeviceConfirmation).link
                _tokenState.value = link
                Log.d(TAG, link)

            }
            is TdApi.AuthorizationStateReady -> {
                //Log.d(TAG, "onResult: AuthorizationStateReady -> state = AUTHENTICATED")
                _authorizationState.value = Authorization.AUTHORIZED
            }
            is TdApi.AuthorizationStateLoggingOut -> {
                Log.d(TAG, "onResult: AuthorizationStateLoggingOut")
                _authorizationState.value = Authorization.UNAUTHORIZED
            }
        }
    }

}