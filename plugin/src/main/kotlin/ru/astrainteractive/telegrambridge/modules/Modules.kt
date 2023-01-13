package ru.astrainteractive.telegrambridge.modules

import org.bukkit.Bukkit
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import ru.astrainteractive.astralibs.di.*
import ru.astrainteractive.telegrambridge.bot.BridgeBot
import ru.astrainteractive.telegrambridge.events.events.DiscordSRVEvent
import ru.astrainteractive.telegrambridge.messaging.MessageController
import ru.astrainteractive.telegrambridge.utils.Files
import ru.astrainteractive.telegrambridge.utils.PluginConfiguration
import ru.astrainteractive.telegrambridge.utils.PluginDependency
import ru.astrainteractive.telegrambridge.utils.PluginTranslation


object Modules {
    val PluginConfigModule = reloadable {
        PluginConfiguration(Files.configFile.fileConfiguration)
    }
    val TranslationModule = reloadable {
        PluginTranslation()
    }
    val bridgeBotModule = module {
        BridgeBot(PluginConfigModule)
    }
    val telegramBotApiModule = module {
        val telegramBridgeBot by bridgeBotModule
        TelegramBotsApi(DefaultBotSession::class.java).also {
            it.registerBot(telegramBridgeBot)
        }
    }
    val messageControllerModule = module {
        MessageController()
    }
    val discordSrvEvent = module {
        DiscordSRVEvent()
    }
}

