package xyz.tolvanen.weargram

import androidx.navigation.NavBackStackEntry

sealed class Screen(val route: String) {

    object Home : Screen("home")

    object Login : Screen("login")

    object Chat : Screen("chat/{chatId}") {
        fun buildRoute(chatId: Long): String = "chat/${chatId}"
        fun getChatId(entry: NavBackStackEntry): Long =
            entry.arguments!!.getString("chatId")?.toLong()
                ?: throw IllegalArgumentException("chatId argument missing.")
    }

    object MessageOptions : Screen("messageOptions/{chatId}") {
        fun buildRoute(chatId: Long): String = "messageOptions/${chatId}"
        fun getChatId(entry: NavBackStackEntry): Long =
            entry.arguments!!.getString("chatId")?.toLong()
                ?: throw IllegalArgumentException("chatId argument missing.")

    }

    //object CreateChat : Screen("createChat")
}
