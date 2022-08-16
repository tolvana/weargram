package xyz.tolvanen.weargram

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import org.drinkless.td.libcore.telegram.TdApi
import java.util.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WeargramModule {
    @Provides
    fun provideTdlibParameters(@ApplicationContext context: Context): TdApi.TdlibParameters {
        return TdApi.TdlibParameters().apply {
            // Obtain application identifier hash for Telegram API access at https://my.telegram.org
            apiId = 94575
            apiHash = "a3406de8d171bb422bb6ddf3bbd800e2"
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
}