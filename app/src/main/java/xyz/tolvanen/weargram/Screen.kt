package xyz.tolvanen.weargram

import androidx.navigation.NavBackStackEntry
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
