package xyz.tolvanen.weargram.ui.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.wear.compose.material.*

@Composable
fun MenuItem(
    title: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        modifier = modifier.fillMaxWidth(0.9f),
        onClick = onClick,
        label = { Text(title) },
        icon = { Icon(imageVector = imageVector, contentDescription = title) },
        colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface)
    )

}

@Composable
fun MenuItem(
    title: String,
    iconPainter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        modifier = modifier.fillMaxWidth(0.9f),
        onClick = onClick,
        label = { Text(title) },
        icon = { Icon(painter = iconPainter, contentDescription = title) },
        colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface)
    )

}
