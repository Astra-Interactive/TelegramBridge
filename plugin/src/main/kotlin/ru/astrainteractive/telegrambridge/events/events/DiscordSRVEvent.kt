package ru.astrainteractive.telegrambridge.events.events

import github.scarsz.discordsrv.DiscordSRV
import github.scarsz.discordsrv.api.Subscribe
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent
import github.scarsz.discordsrv.dependencies.jda.api.events.message.MessageReceivedEvent
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.async.PluginScope
import ru.astrainteractive.astralibs.di.getValue
import ru.astrainteractive.telegrambridge.messaging.Message
import ru.astrainteractive.telegrambridge.modules.Modules
import org.bukkit.event.Listener;
import ru.astrainteractive.astralibs.events.EventListener
import ru.astrainteractive.astralibs.events.EventManager

class DiscordSRVEvent: EventListener {
    private val messageController by Modules.messageControllerModule
    private val translation by Modules.TranslationModule


    @Subscribe
    public fun discordMessageProcessed(event: DiscordGuildMessagePostProcessEvent) {
        if (event.message.isWebhookMessage) return
        if (event.message.author.isBot) return
        PluginScope.launch(Dispatchers.IO) {
            val authorName = event.member?.nickname ?: event.member?.effectiveName ?: event.author.name ?: "Безымянный"
            val telegramFormat = translation.telegramMessageFormat(authorName, event.message.contentRaw)
            messageController.sendToTelegram(Message.Text(telegramFormat,Message.MessageFrom.DISCORD),)
        }
    }

    override fun onEnable(manager: EventManager): EventListener {
        DiscordSRV.api.subscribe(this)
        return super.onEnable(manager)
    }

    override fun onDisable() {
        super.onDisable()
        DiscordSRV.api.unsubscribe(this)
    }
}