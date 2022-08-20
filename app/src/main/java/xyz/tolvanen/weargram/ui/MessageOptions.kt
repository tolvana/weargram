package xyz.tolvanen.weargram.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import xyz.tolvanen.weargram.R

@Composable
fun MessageOptionsScreen(navController: NavController, chatId: Long) {

    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 0.dp),

    ) {

        item {
            MessageOption(
                title = "Sticker",
                iconPainter = painterResource(id = R.drawable.baseline_emoji_emotions_24),
                onClick = {}
            )
        }

        item {
            MessageOption(
                title = "Location",
                iconPainter = painterResource(id = R.drawable.baseline_location_on_24),
                onClick = {}
            )
        }

        item {
            MessageOption(
                title = "Audio message",
                iconPainter = painterResource(id = R.drawable.baseline_mic_24),
                onClick = {}
            )
        }

    }

}

@Composable
fun MessageOption(title: String, iconPainter: Painter, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        label = { Text(title) },
        icon = { Icon(painter = iconPainter, contentDescription = title) },
        colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface)
    )

}
