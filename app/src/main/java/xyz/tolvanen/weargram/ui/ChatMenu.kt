package xyz.tolvanen.weargram.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.R
import xyz.tolvanen.weargram.Screen

@Composable
fun ChatMenuScreen(chatId: Long, viewModel: ChatMenuViewModel, navController: NavController) {

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
            MessageMenuItem(
                title = "Sticker",
                iconPainter = painterResource(id = R.drawable.baseline_emoji_emotions_24),
                onClick = {}
            )
        }

        item {
            MessageMenuItem(
                title = "Location",
                iconPainter = painterResource(id = R.drawable.baseline_location_on_24),
                onClick = {}
            )
        }

        item {
            MessageMenuItem(
                title = "Audio message",
                iconPainter = painterResource(id = R.drawable.baseline_mic_24),
                onClick = {}
            )
        }
    }
}

@Composable
fun MessageMenuItem(title: String, iconPainter: Painter, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth(0.9f),
        onClick = onClick,
        label = { Text(title) },
        icon = { Icon(painter = iconPainter, contentDescription = title) },
        colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface)
    )

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
    MessageMenuItem(
        title = "User Info",
        iconPainter = painterResource(id = R.drawable.baseline_info_24),
        onClick = { navController.navigate(Screen.Info.buildRoute(userId)) }
    )
}

@Composable
fun GroupInfo(groupId: Long, navController: NavController) {
    MessageMenuItem(
        title = "Group Info",
        iconPainter = painterResource(id = R.drawable.baseline_info_24),
        onClick = {}//{ navController.navigate(Screen.Info.buildGroupRoute(groupId)) }
    )
}

@Composable
fun ChannelInfo(channelId: Long, navController: NavController) {
    MessageMenuItem(
        title = "Channel Info",
        iconPainter = painterResource(id = R.drawable.baseline_info_24),
        onClick = {}//{ navController.navigate(Screen.Info.buildChannelRoute(channelId)) }
    )
}

