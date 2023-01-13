package ru.astrainteractive.telegrambridge

import CommandManager
import kotlinx.coroutines.cancel
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import ru.astrainteractive.astralibs.AstraLibs
import ru.astrainteractive.astralibs.Logger
import ru.astrainteractive.astralibs.async.PluginScope
import ru.astrainteractive.astralibs.di.getValue
import ru.astrainteractive.astralibs.events.GlobalEventManager
import ru.astrainteractive.telegrambridge.events.EventHandler
import ru.astrainteractive.telegrambridge.events.events.DiscordSRVEvent
import ru.astrainteractive.telegrambridge.modules.Modules
import ru.astrainteractive.telegrambridge.utils.Files
import ru.astrainteractive.telegrambridge.utils.PluginDependency
import ru.astrainteractive.telegrambridge.utils.Singleton


/**
 * Initial class for your plugin
 */
class TelegramBridge : JavaPlugin() {
    companion object : Singleton<TelegramBridge>()

    /**
     * Class for handling all of your events
     */
    private val eventHandler by lazy { EventHandler() }
    private val discordSrvEvent by Modules.discordSrvEvent

    /**
     * This method called when server starts or PlugMan load plugin.
     */
    override fun onEnable() {
        super.onEnable()
        instance = this
        AstraLibs.rememberPlugin(instance)
        Logger.prefix = "TelegramBridge"
        eventHandler
        CommandManager()
        Modules.telegramBotApiModule.value
        Bukkit.getPluginManager().getPlugin("DiscordSRV")?.let {
            discordSrvEvent.onEnable(GlobalEventManager)
        } ?: kotlin.run {
            logger.warning("DiscordSRV not installed")
        }
    }

    /**
     * This method called when server is shutting down or when PlugMan disable plugin.
     */
    override fun onDisable() {
        eventHandler.onDisable()
        HandlerList.unregisterAll(this)
        GlobalEventManager.onDisable()
        PluginScope.cancel()
        PluginScope.coroutineContext.cancel()
        Bukkit.getPluginManager().getPlugin("DiscordSRV")?.let {
            discordSrvEvent.onDisable()
        }
    }

    /**
     * As it says, function for plugin reload
     */
    fun reloadPlugin() {
        Files.configFile.reload()
        Modules.PluginConfigModule.reload()
        Modules.TranslationModule.reload()
    }

}


