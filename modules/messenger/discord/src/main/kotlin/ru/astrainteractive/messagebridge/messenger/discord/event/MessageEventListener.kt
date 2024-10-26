package ru.astrainteractive.messagebridge.messenger.discord.event

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.MinecraftBridge
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent
import ru.astrainteractive.messagebridge.messenger.discord.event.core.DiscordEventListener

internal class MessageEventListener(
    private val configKrate: Krate<PluginConfiguration>,
    private val translationKrate: Krate<PluginTranslation>,
    private val telegramMessageController: MessageController,
    private val minecraftMessageController: MessageController,
    private val minecraftBridge: MinecraftBridge,
    private val linkApi: LinkApi
) : ListenerAdapter(),
    DiscordEventListener,
    CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO),
    Logger by JUtiltLogger("MessageEventListener") {

    private fun onPrivateMessage(event: MessageReceivedEvent) {
        val member = event.member ?: return
        launch {
            val code = event.message.contentRaw.toIntOrNull() ?: -1
            val response = linkApi.linkDiscord(code, member)
            val message = response.asMessage(translationKrate.cachedValue.link).raw
            event.message.reply(message).queue()
        }
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        super.onGuildMemberRemove(event)
        launch { linkApi.userLeaveDiscord(event.user) }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.isWebhookMessage) return
        if (event.author.isBot) return
        if (event.channelType == ChannelType.PRIVATE) {
            onPrivateMessage(event)
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
        if (event.message.contentRaw.startsWith("/link")) {
            val member = event.member ?: return
            launch {
                val code = event.message.contentRaw.replace("/link ", "")
                    .toIntOrNull()
                    ?: -1
                val response = linkApi.linkDiscord(code, member)
                val message = response.asMessage(translationKrate.cachedValue.link).raw
                event.message.reply(message).queue()
            }
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
