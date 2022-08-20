package xyz.tolvanen.weargram.ui.login

import android.app.RemoteInput
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender

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
