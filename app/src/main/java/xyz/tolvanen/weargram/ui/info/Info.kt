package xyz.tolvanen.weargram.ui.info

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import org.drinkless.td.libcore.telegram.TdApi

@Composable
fun InfoScreen(userId: Long, viewModel: InfoViewModel, navController: NavController) {

    UserInfoScreen(userId, viewModel, navController)
}

@Composable
fun UserInfoScreen(userId: Long, viewModel: InfoViewModel, navController: NavController) {

    viewModel.getUser(userId)?.also { user ->
        UserInfoScaffold(user, viewModel)
    }
}

@Composable
fun UserInfoScaffold(user: TdApi.User, viewModel: InfoViewModel) {

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
    ) {
        ScalingLazyColumn {
            user.profilePhoto?.also {
                item { UserImage(it, viewModel) }
            }
            //item { Name(user) }
            //item { PhoneNumber(user) }
            //item { SendMessage(user) }
        }

    }
}

@Composable
fun UserImage(photo: TdApi.ProfilePhoto, viewModel: InfoViewModel) {

    val thumbnailBitmap = remember {
        photo.minithumbnail?.let { thumbnail ->
            val data = thumbnail.data
            val aspectRatio = thumbnail.width.toFloat() / thumbnail.height.toFloat()
            val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
            Bitmap.createScaledBitmap(bmp, 200, (200 / aspectRatio).toInt(), true).asImageBitmap()
        }
    }

    viewModel.fetchPhoto(photo).collectAsState(null).value?.also {
        Image(it, null)
    } ?: run {
        thumbnailBitmap?.also {
            Box {
                Image(it, null)
                CircularProgressIndicator()
            }
        }

    }

}

