package xyz.tolvanen.weargram

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import org.drinkless.td.libcore.telegram.TdApi
import xyz.tolvanen.weargram.client.Authenticator
import xyz.tolvanen.weargram.client.ChatProvider
import xyz.tolvanen.weargram.client.TelegramClient
import java.util.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WeargramModule {
    @Provides
    fun provideTdlibParameters(@ApplicationContext context: Context): TdApi.TdlibParameters {
        return TdApi.TdlibParameters().apply {
            // Obtain application identifier hash for Telegram API access at https://my.telegram.org
            apiId = context.resources.getInteger(R.integer.api_id)
            apiHash = context.getString(R.string.api_hash)
            useMessageDatabase = true
            useSecretChats = true
            systemLanguageCode = Locale.getDefault().language
            databaseDirectory = context.filesDir.absolutePath
            deviceModel = Build.MODEL
            systemVersion = Build.VERSION.RELEASE
            applicationVersion = "0.1"
            enableStorageOptimizer = true
        }
    }

    @Singleton
    @Provides
    fun provideTelegramClient(parameters: TdApi.TdlibParameters) = TelegramClient(parameters)

    @Singleton
    @Provides
    fun provideAuthenticator(telegramClient: TelegramClient) = Authenticator(telegramClient)

    @Singleton
    @Provides
    fun provideChatProvider(telegramClient: TelegramClient) = ChatProvider(telegramClient)

}