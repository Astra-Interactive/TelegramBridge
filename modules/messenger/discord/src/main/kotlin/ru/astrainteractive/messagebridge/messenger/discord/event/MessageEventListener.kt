package ru.astrainteractive.messagebridge.messenger.discord.event

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.MinecraftBridge
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent
import ru.astrainteractive.messagebridge.messenger.discord.event.core.DiscordEventListener

internal class MessageEventListener(
    private val configKrate: Krate<PluginConfiguration>,
    private val telegramMessageController: MessageController,
    private val minecraftMessageController: MessageController,
    private val minecraftBridge: MinecraftBridge
) : ListenerAdapter(),
    DiscordEventListener,
    CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO),
    Logger by JUtiltLogger("MessageEventListener") {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.isWebhookMessage) return
        if (event.author.isBot) return
        if (event.channelType == ChannelType.PRIVATE) {
            // todo bot message received
            return
        }
        if (event.message.channelId != configKrate.cachedValue.jdaConfig.channelId) return
        if (event.message.contentRaw == "!vanilla") {
            info { "#onMessageReceived !vanilla executed" }
            val players = minecraftBridge.getOnlinePlayers()
            val message = players.joinToString(
                ", ",
                prefix = "Сейчас онлайн ${players.size} игроков\n"
            )
            event.message.reply(message).queue()
            return
        }
        val serverEvent = ServerEvent.Text.Discord(
            author = event.member?.nickname ?: event.author.name,
            text = event.message.contentRaw
        )
        launch(Dispatchers.IO) {
            telegramMessageController.send(serverEvent)
            minecraftMessageController.send(serverEvent)
        }
    }
}
