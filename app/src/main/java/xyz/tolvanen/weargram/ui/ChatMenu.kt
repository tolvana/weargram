package xyz.tolvanen.weargram.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.R
import xyz.tolvanen.weargram.Screen
import xyz.tolvanen.weargram.ui.util.MenuItem
import xyz.tolvanen.weargram.ui.util.YesNoDialog

@Composable
fun ChatMenuScreen(chatId: Long, viewModel: ChatMenuViewModel, navController: NavController) {

    val chat = viewModel.getChat(chatId)

    val showLeaveDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }

    if (showDeleteDialog.value) {
        YesNoDialog(
            text = "Delete chat?",
            onYes = {
                viewModel.deleteChat(chatId)
                navController.popBackStack()
            },
            onNo = { showDeleteDialog.value = false }
        )
    } else if (showLeaveDialog.value) {
        YesNoDialog(
            text = "Leave chat?",
            onYes = {
                viewModel.leaveChat(chatId)
                navController.popBackStack()
            },
            onNo = { showLeaveDialog.value = false }
        )
    } else {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 0.dp),

            ) {

            item {
                Info(chatId, viewModel, navController)
            }

            item {
                Text("Send message")
            }

            item {
                MenuItem(
                    title = "Sticker",
                    iconPainter = painterResource(id = R.drawable.baseline_emoji_emotions_24),
                    onClick = {}
                )
            }

            item {
                MenuItem(
                    title = "Location",
                    iconPainter = painterResource(id = R.drawable.baseline_location_on_24),
                    onClick = {}
                )
            }

            item {
                MenuItem(
                    title = "Audio message",
                    iconPainter = painterResource(id = R.drawable.baseline_mic_24),
                    onClick = {}
                )
            }
            item {
                Spacer(modifier = Modifier.height(5.dp))
            }

            chat?.also {
                if (it.canBeDeletedForAllUsers) {
                    item {
                        MenuItem(
                            title = "Delete",
                            imageVector = Icons.Outlined.Delete,
                            onClick = { /* viewModel.deleteChat(it) */ }
                        )
                    }
                }

                item {
                    MenuItem(
                        title = "Leave",
                        imageVector = Icons.Outlined.Logout,
                        onClick = { /* viewModel.leaveChat(it) */ }
                    )
                }

            }
        }

    }

}

@Composable
fun Info(chatId: Long, viewModel: ChatMenuViewModel, navController: NavController) {
    viewModel.getChat(chatId)?.also { chat ->
        when (val chatType = chat.type) {
            is TdApi.ChatTypePrivate -> UserInfo(chatType.userId, navController)
            is TdApi.ChatTypeSecret -> UserInfo(chatType.userId, navController)
            is TdApi.ChatTypeBasicGroup -> GroupInfo(chatType.basicGroupId, navController)
            is TdApi.ChatTypeSupergroup -> ChannelInfo(chatType.supergroupId, navController)

        }
    }


}

@Composable
fun UserInfo(userId: Long, navController: NavController) {
    MenuItem(
        title = "User Info",
        iconPainter = painterResource(id = R.drawable.baseline_info_24),
        onClick = { navController.navigate(Screen.Info.buildRoute("user", userId)) }
    )
}

@Composable
fun GroupInfo(groupId: Long, navController: NavController) {
    MenuItem(
        title = "Group Info",
        iconPainter = painterResource(id = R.drawable.baseline_info_24),
        onClick = { navController.navigate(Screen.Info.buildRoute("group", groupId)) }
    )
}

@Composable
fun ChannelInfo(channelId: Long, navController: NavController) {
    MenuItem(
        title = "Channel Info",
        iconPainter = painterResource(id = R.drawable.baseline_info_24),
        onClick = { navController.navigate(Screen.Info.buildRoute("channel", channelId)) }
    )
}

