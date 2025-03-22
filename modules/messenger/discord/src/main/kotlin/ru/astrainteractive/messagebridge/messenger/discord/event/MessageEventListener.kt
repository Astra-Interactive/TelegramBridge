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
import ru.astrainteractive.messagebridge.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.event.core.DiscordEventListener
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await

internal class MessageEventListener(
    private val configKrate: Krate<PluginConfiguration>,
    private val translationKrate: Krate<PluginTranslation>,
    private val onlinePlayersProvider: OnlinePlayersProvider,
    private val linkApi: LinkApi
) : ListenerAdapter(),
    DiscordEventListener,
    CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO),
    Logger by JUtiltLogger("MessageEventListener") {

    private suspend fun onPrivateMessage(event: MessageReceivedEvent) {
        val member = event.member ?: return
        val code = event.message.contentRaw.toIntOrNull() ?: -1
        val response = linkApi.linkDiscord(code, member)
        val message = response.asMessage(translationKrate.cachedValue.link).raw
        event.message.reply(message).await()
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        super.onGuildMemberRemove(event)
        launch { linkApi.userLeaveDiscord(event.user) }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.isWebhookMessage) return
        if (event.author.isBot) return
        if (event.channelType == ChannelType.PRIVATE) {
            launch { onPrivateMessage(event) }
            return
        }
        if (event.message.channelId != configKrate.cachedValue.jdaConfig.channelId) return
        if (event.message.contentRaw == "!vanilla") {
            info { "#onMessageReceived !vanilla executed" }
            val players = onlinePlayersProvider.provide()
            val message = players.joinToString(
                ", ",
                prefix = "Сейчас онлайн ${players.size} игроков\n"
            )
            launch { event.message.reply(message).await() }
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
                event.message.reply(message).await()
            }
        }
        val bEvent = Text.Discord(
            author = event.member?.nickname ?: event.author.name,
            text = event.message.contentRaw,
            authorId = event.author.idLong
        )
        launch(Dispatchers.IO) {
            BEventChannel.consume(bEvent)
        }
    }
}
