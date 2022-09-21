package xyz.tolvanen.weargram.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.runtime.Composable
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert

@Composable
fun YesNoDialog(text: String, onYes: () -> Unit, onNo: () -> Unit) {

    Alert(
        title = { Text(text) },
        negativeButton = {
            Button(
                onClick = onNo,
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = null)
            }
        },
        positiveButton = {
            Button(
                onClick = onYes,
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = MaterialTheme.colors.primaryVariant)
            ) {
                Icon(imageVector = Icons.Outlined.Done, contentDescription = null)
            }
        }) {

    }
}