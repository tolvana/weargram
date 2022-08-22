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
fun MessageMenuScreen(navController: NavController, chatId: Long) {

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 0.dp),

    ) {

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
