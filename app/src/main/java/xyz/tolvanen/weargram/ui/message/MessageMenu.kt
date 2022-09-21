package xyz.tolvanen.weargram.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.ui.util.MenuItem
import xyz.tolvanen.weargram.ui.util.YesNoDialog

@Composable
fun MessageMenuScreen(
    navController: NavController,
    chatId: Long,
    messageId: Long,
    viewModel: MessageMenuViewModel
) {

    val message = viewModel.getMessage(chatId, messageId).collectAsState(initial = null)

    message.value?.also {
        MessageMenuScaffold(chatId, it, navController, viewModel)
    }

}

@Composable
fun MessageMenuScaffold(
    chatId: Long,
    message: TdApi.Message,
    navController: NavController,
    viewModel: MessageMenuViewModel
) {

    val showDeleteDialog = remember { mutableStateOf(false) }

    if (showDeleteDialog.value) {
        YesNoDialog(text = "Delete message?",
            onYes = {
                viewModel.deleteMessage(chatId, message.id)
                navController.popBackStack()
            },
            onNo = { showDeleteDialog.value = false }
        )
    } else {
        Scaffold(
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        ) {
            ScalingLazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {

                item { DeleteItem(onClick = { showDeleteDialog.value = true }) }

            }

        }
    }


}

@Composable
fun DeleteItem(onClick: () -> Unit) {
    MenuItem(
        title = "Delete",
        imageVector = Icons.Outlined.Delete,
        onClick = onClick
    )
}