package xyz.tolvanen.weargram.ui.login

import android.app.RemoteInput
import android.content.Intent
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import xyz.tolvanen.weargram.R

@Composable
fun LoginScreen(viewModel: LoginViewModel, loggedIn: () -> Unit) {

    val loginState by viewModel.loginState

    when (loginState) {
        is LoginState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is LoginState.SetNumber -> PhoneNumberScreen(viewModel) { viewModel.setNumber(it) }
        is LoginState.ShowQrCode -> QrCodeScreen(viewModel)
        is LoginState.SetCode -> CodeScreen { viewModel.setCode(it) }
        is LoginState.SetPassword -> PasswordScreen { viewModel.setPassword(it) }
        is LoginState.Authorized -> loggedIn()
    }
}

@Composable
fun PhoneNumberScreen(viewModel: LoginViewModel, onEntry: (String) -> Unit) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let { data ->
                val results: Bundle = RemoteInput.getResultsFromIntent(data)
                val input: CharSequence? = results.getCharSequence("input")
                onEntry(input.toString())
            }
        }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    )
    {
        Text(
            "Login options",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Chip(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(4.dp),
            onClick = { viewModel.requestQrCode() },
            label = { Text("Scan QR code") },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_qr_code_24),
                    contentDescription = null
                )
            },
            colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface)
        )

        Chip(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(4.dp),
            onClick = {
                val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                val remoteInputs: List<RemoteInput> = listOf(
                    RemoteInput.Builder("input")
                        .setLabel("")
                        .wearableExtender {
                            setEmojisAllowed(false)
                            setInputActionType(EditorInfo.IME_ACTION_DONE)
                        }.build()
                )

                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)

                launcher.launch(intent)

            },
            label = { Text("Enter phone number") },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_phone_24),
                    contentDescription = null
                )
            },
            colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface)
        )
    }
}

@Composable
fun QrCodeScreen(viewModel: LoginViewModel) {

    val qrCode by viewModel.qrCode

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        qrCode?.let {
            Image(
                it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.6f)
                    .align(Alignment.Center)
            )
        } ?: CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

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
            colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface),
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
    data class ShowQrCode(val previousError: Throwable? = null) : LoginState()
    data class SetCode(val previousError: Throwable? = null) : LoginState()
    data class SetPassword(val previousError: Throwable? = null) : LoginState()
    object Authorized : LoginState()
}
