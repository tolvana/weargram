package xyz.tolvanen.weargram.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavHostState
import xyz.tolvanen.weargram.Screen
import xyz.tolvanen.weargram.theme.WeargramTheme

@Composable
fun App() {
    WeargramTheme {
        val navController = rememberSwipeDismissableNavController()
        MainNavHost(navController)
    }
}

@Composable
private fun MainNavHost(navController: NavHostController) {
    SwipeDismissableNavHost(navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            Log.d("top", "home: ${navController.backQueue.joinToString { entry -> entry.destination.toString()}}")
            HomeScreen(navController, hiltViewModel(it))
        }

        composable(Screen.Login.route) {
            Log.d("top", "login: ${navController.backQueue.joinToString { entry -> entry.destination.toString()}}")
            LoginScreen(hiltViewModel(it)) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        composable(Screen.Chat.route) {
            val chatId = Screen.Chat.getChatId(it)
            //viewModel.setChatId(chatId)
            Log.d("top", "chat: ${navController.backQueue.joinToString { entry -> entry.destination.toString()}}")
            ChatScreen(
                navController = navController,
                chatId = chatId,
                viewModel = hiltViewModel(it)
            )
        }

    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    App()
}
