package xyz.tolvanen.weargram.ui.info

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.R
import xyz.tolvanen.weargram.Screen
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserInfoScreen(userId: Long, viewModel: InfoViewModel, navController: NavController) {

    viewModel.getUser(userId)?.also { user ->
        UserInfoScaffold(user, viewModel, navController)
    }
}

@Composable
fun UserInfoScaffold(user: TdApi.User, viewModel: InfoViewModel, navController: NavController) {
    //val listState = rememberScalingLazyListState()

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
    ) {
        ScalingLazyColumn(
            //state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    user.profilePhoto?.also {
                        InfoImage(it.big, it.minithumbnail, viewModel)
                    } ?: run {
                        PlaceholderInfoImage(painterResource(R.drawable.baseline_person_24))
                    }

                }
            }

            item { Name(user) }
            item { UserStatus(user) }
            item { Username(user) }
            item { PhoneNumber(user) }
            item { SendMessage(user, viewModel, navController) }
        }

    }
}

@Composable
fun Name(user: TdApi.User) {
    Text(
        user.firstName + " " + user.lastName,
        style = MaterialTheme.typography.title2,
        modifier = Modifier.padding(top = 5.dp)
    )
}

@Composable
fun Username(user: TdApi.User) {
    if (user.username != "") {
        Text("@${user.username}")
    }
}

@Composable
fun UserStatus(user: TdApi.User) {
    // TODO: get user status updates here
    val locale = LocalContext.current.resources.configuration.locales[0]
    val text = when (val status = user.status) {
        is TdApi.UserStatusOnline -> {
            if (status.expires * 1000L > Calendar.getInstance().time.time) {
                "Online"
            } else {
                "last seen ${timeDescription(status.expires, locale)}"
            }

        }
        is TdApi.UserStatusOffline -> "last seen ${timeDescription(status.wasOnline, locale)}"
        is TdApi.UserStatusRecently -> "last seen recently"
        is TdApi.UserStatusLastWeek -> "last seen within a week"
        is TdApi.UserStatusLastMonth -> "last seen within a month"
        else -> ""
    }

    if (text != "") {
        Text(text)
    }
}

fun timeDescription(timestamp: Int, locale: Locale): String {
    val date = Date(timestamp * 1000L)
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val lastYear = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }

    return if (date.after(yesterday.time)) {
        "at " + DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
    } else if (date.after(lastYear.time)) {
        SimpleDateFormat("MMM dd", locale).format(date) + " at " + DateFormat.getTimeInstance(
            DateFormat.SHORT
        ).format(date)
    } else {
        DateFormat.getDateInstance(DateFormat.SHORT).format(date)
    }
}

@Composable
fun PhoneNumber(user: TdApi.User) {
    val context = LocalContext.current
    if (user.phoneNumber != "") {
        val phoneNumber = "+${user.phoneNumber}"
        Text(phoneNumber, modifier = Modifier
            .padding(top = 5.dp)
            .clickable {
                val phoneIntent = Intent(
                    Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null)
                )
                phoneIntent
                    .resolveActivity(context.packageManager)
                    ?.also {
                        ContextCompat.startActivity(context, phoneIntent, null)
                    }
            })
    }
}

@Composable
fun SendMessage(user: TdApi.User, viewModel: InfoViewModel, navController: NavController) {

    val chat = viewModel.getPrivateChat(user.id).collectAsState(null)

    Chip(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(top = 5.dp),
        onClick = {
            chat.value?.also {
                navController.navigate(Screen.Chat.buildRoute(it.id)) {
                    popUpTo(Screen.Home.route)
                }
            }
        },
        label = { Text("Send message") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.baseline_message_24),
                contentDescription = null
            )
        },
        colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface)
    )
}
