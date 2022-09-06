package xyz.tolvanen.weargram.ui.info

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.material.CircularProgressIndicator
import org.drinkless.td.libcore.telegram.TdApi

@Composable
fun InfoScreen(type: String, id: Long, viewModel: InfoViewModel, navController: NavController) {

    when (type) {
        "user" -> UserInfoScreen(id, viewModel, navController)
        "group" -> GroupInfoScreen(id, viewModel, navController)
        //"channel" -> ChannelInfoScreen(id, viewModel, navController)
        else -> {
            navController.popBackStack()
        }
    }
}

@Composable
fun InfoImage(
    photo: TdApi.File,
    thumbnail: TdApi.Minithumbnail?,
    viewModel: InfoViewModel,
    imageSize: Dp = 120.dp
) {

    val thumbnailBitmap = remember {
        thumbnail?.let {
            val data = thumbnail.data
            val aspectRatio = thumbnail.width.toFloat() / thumbnail.height.toFloat()
            val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
            Bitmap.createScaledBitmap(bmp, 400, (400 / aspectRatio).toInt(), true).asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
    ) {

        val imageModifier = Modifier
            .clip(CircleShape)
            .align(Alignment.Center)
            .size(imageSize)

        viewModel.fetchPhoto(photo).collectAsState(null).value?.also {
            Image(it, null, modifier = imageModifier)
        } ?: run {
            thumbnailBitmap?.also {
                Image(it, null, modifier = imageModifier)
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

}

@Composable
fun PlaceholderInfoImage(painter: Painter, imageSize: Dp = 120.dp) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color = Color(0xFF888888))
    ) {

        val imageModifier =
            Image(
                painter, null, modifier = Modifier
                    .clip(CircleShape)
                    .align(Alignment.Center)
                    .size(imageSize)
            )

    }

}



