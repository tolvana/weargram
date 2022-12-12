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

    object ChatMenu : Screen("chatMenu/{chatId}") {
        fun buildRoute(chatId: Long): String = "chatMenu/${chatId}"
        fun getChatId(entry: NavBackStackEntry): Long? =
            entry.arguments?.getString("chatId")?.toLong()
    }

    object MessageMenu : Screen("messageMenu/{chatId}/{messageId}") {
        fun buildRoute(chatId: Long, messageId: Long): String = "messageMenu/$chatId/$messageId"
        fun getChatId(entry: NavBackStackEntry): Long? =
            entry.arguments?.getString("chatId")?.toLong()

        fun getMessageId(entry: NavBackStackEntry): Long? =
            entry.arguments?.getString("messageId")?.toLong()
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

    object Map : Screen("map/{latitude}/{longitude}") {
        fun buildRoute(latitude: Double, longitude: Double): String =
            "map/$latitude/$longitude"

        fun getCoordinates(entry: NavBackStackEntry): Pair<Double, Double> =
            Pair(
                entry.arguments?.getString("latitude")?.toDouble() ?: 0.0,
                entry.arguments?.getString("longitude")?.toDouble() ?: 0.0
            )

    }
    //object CreateChat : Screen("createChat")
}
