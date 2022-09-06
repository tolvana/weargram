package xyz.tolvanen.weargram

import androidx.navigation.NavBackStackEntry
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {

    object Home : Screen("home")
    object MainMenu : Screen("mainMenu")
    object Login : Screen("login")

    object Chat : Screen("chat/{chatId}") {
        fun buildRoute(chatId: Long): String = "chat/${chatId}"
        fun getChatId(entry: NavBackStackEntry): Long? =
            entry.arguments?.getString("chatId")?.toLong()
    }

    object MessageMenu : Screen("messageMenu/{chatId}") {
        fun buildRoute(chatId: Long): String = "messageMenu/${chatId}"
        fun getChatId(entry: NavBackStackEntry): Long? =
            entry.arguments?.getString("chatId")?.toLong()

    }

    object Info : Screen("info/{chatId}") {
        fun buildRoute(chatId: Long): String = "info/${chatId}"
        fun getChatId(entry: NavBackStackEntry): Long? =
            entry.arguments?.getString("chatId")?.toLong()


    }

    object Video : Screen("video/{path}") {
        fun buildRoute(path: String): String =
            "video/${URLEncoder.encode(path, StandardCharsets.UTF_8.toString())}"

        fun getPath(entry: NavBackStackEntry): String = URLDecoder.decode(
            entry.arguments!!.getString("path"),
            StandardCharsets.UTF_8.toString()
        )

    }

    //object CreateChat : Screen("createChat")
}
