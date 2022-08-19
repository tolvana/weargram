package xyz.tolvanen.weargram.ui

import android.app.RemoteInput
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import xyz.tolvanen.weargram.client.Authenticator
import xyz.tolvanen.weargram.client.Authorization
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val authenticator: Authenticator) : ViewModel() {

    val loginState = mutableStateOf<LoginState>(LoginState.SetNumber())

    val qrCode = mutableStateOf<Bitmap?>(null)

    init {
        authenticator.authorizationState.onEach {
            Log.d("kek", "here with $it")
            when (it) {
                Authorization.UNAUTHORIZED -> {
                    loginState.value = LoginState.Loading
                }
                Authorization.WAIT_NUMBER, Authorization.INVALID_NUMBER -> {
                    loginState.value = LoginState.SetNumber()
                }
                Authorization.WAIT_CODE, Authorization.INVALID_CODE -> {
                    loginState.value = LoginState.SetCode()
                }
                Authorization.WAIT_PASSWORD, Authorization.INVALID_PASSWORD -> {
                    loginState.value = LoginState.SetPassword()
                }
                Authorization.AUTHORIZED -> {
                    loginState.value = LoginState.Authorized
                }
            }
        }.launchIn(viewModelScope)

        authenticator.tokenState.onEach {
            it?.also { generateQrCode(it) }
        }.launchIn(viewModelScope)

    }

    private fun generateQrCode(token: String) {
        val size = 512f
        val mat = Encoder.encode(token, ErrorCorrectionLevel.L).matrix
        val bmpSize = ((size * (mat.height + 2)) / mat.height).toInt()
        val offset = (bmpSize - size) / 2
        val bmp = Bitmap.createBitmap(bmpSize, bmpSize, Bitmap.Config.RGB_565)
        for (i in 0 until bmpSize) {
            val iMat = (i - offset) * mat.width / size
            for (j in 0 until bmpSize) {
                val jMat = (j - offset) * mat.height / size

                val pixelColor =
                    (if (iMat >= 0 && iMat < mat.width
                        && jMat >= 0 && jMat < mat.height
                        && mat[iMat.toInt(), jMat.toInt()] != 0.toByte()
                    )
                        Color.BLACK
                    else Color.WHITE)

                bmp.setPixel(i, j, pixelColor)
            }
        }

        qrCode.value = bmp
    }

    fun setNumber(number: String) {
        authenticator.setPhoneNumber(number)
        loginState.value = LoginState.Loading
    }

    fun setCode(code: String) {
        authenticator.setCode(code)
        loginState.value = LoginState.Loading
    }

    fun setPassword(password: String) {
        authenticator.setPassword(password)
        loginState.value = LoginState.Loading
    }
}

@Composable
fun LoginScreen(viewModel: LoginViewModel, loggedIn: () -> Unit) {

    val loginState by viewModel.loginState
    val qrCode by viewModel.qrCode

    when (loginState) {
        is LoginState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is LoginState.SetNumber -> PhoneNumberScreen(qrCode) {
            viewModel.setNumber(it)
        }
        is LoginState.SetCode -> CodeScreen {
            viewModel.setCode(it)
        }
        is LoginState.SetPassword -> PasswordScreen {
            viewModel.setPassword(it)
        }
        is LoginState.Authorized -> loggedIn()
    }
}

@Composable
fun PhoneNumberScreen(qrCode: Bitmap?, onEntry: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        qrCode?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxSize(0.55f),
                contentAlignment = Alignment.Center
            ) {
                //Image(
                //    ColorPainter(androidx.compose.ui.graphics.Color.White),
                //    null,
                //    modifier = Modifier
                //        .fillMaxWidth()
                //        .fillMaxHeight()
                //)
                Image(
                    it.asImageBitmap(), "Scan this QR Code with a logged in Telegram client",
                    modifier = Modifier.fillMaxSize(0.95f)
                )
            }


        } ?: CircularProgressIndicator()

        AuthScreen(
            prompt = "Enter phone number",
            details = "pls enter phone number",
            onEntry = onEntry
        )
    }
}

@Composable
fun CodeScreen(onEntry: (String) -> Unit) {
    AuthScreen(
        prompt = "Enter code",
        onEntry = onEntry
    )
}

@Composable
fun PasswordScreen(onEntry: (String) -> Unit) {
    AuthScreen(
        prompt = "Enter password",
        onEntry = onEntry
    )
}

@Composable
fun AuthScreen(prompt: String, details: String? = null, onEntry: (String) -> Unit) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let { data ->
                val results: Bundle = RemoteInput.getResultsFromIntent(data)
                val input: CharSequence? = results.getCharSequence("input")
                onEntry(input.toString())
            }
        }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Chip(
            //modifier = Modifier.wrapContentSize(Alignment.Center),
            label = { Text(prompt) },
            onClick = {
                val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                val remoteInputs: List<RemoteInput> = listOf(
                    RemoteInput.Builder("input")
                        .setLabel(details ?: "")
                        .wearableExtender {
                            setEmojisAllowed(false)
                            setInputActionType(EditorInfo.IME_ACTION_DONE)
                        }.build()
                )

                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)

                launcher.launch(intent)
            }
        )
    }
}

sealed class LoginState {
    object Loading : LoginState()
    data class SetNumber(val previousError: Throwable? = null) : LoginState()
    data class SetCode(val previousError: Throwable? = null) : LoginState()
    data class SetPassword(val previousError: Throwable? = null) : LoginState()
    object Authorized : LoginState()
}
