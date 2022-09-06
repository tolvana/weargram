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

    object Info : Screen("info/{type}/{id}") {
        fun buildRoute(type: String, id: Long): String = "info/$type/$id"
        fun getId(entry: NavBackStackEntry): Long? =
            entry.arguments?.getString("id")?.toLong()

        fun getType(entry: NavBackStackEntry): String? =
            entry.arguments?.getString("type")

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
