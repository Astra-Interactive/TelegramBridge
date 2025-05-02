package ru.astrainteractive.messagebridge.messenger.discord.messaging

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.messaging.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.MessageFrom
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messaging.tryConsume
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.awaitWithTimeout
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList", "UnusedPrivateProperty", "TooManyFunctions")
internal class DiscordBEventConsumer(
    private val jdaFlow: Flow<JDA>,
    private val webHookClientFlow: Flow<WebhookClient>,
    configKrate: Krate<PluginConfiguration>,
    translationKrate: Krate<PluginTranslation>,
    private val linkingDao: LinkingDao,
    private val onlinePlayersProvider: OnlinePlayersProvider,
    private val dispatchers: KotlinDispatchers
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.Default(dispatchers.IO),
    Logger by JUtiltLogger("MessageBridge-DiscordBEventConsumer") {
    private val config by configKrate

    private suspend fun webHookClient() = webHookClientFlow.first()

    private fun textChannelFlow() = jdaFlow.map { jda ->
        jda.getTextChannelById(config.jdaConfig.channelId)
    }.shareIn(this, SharingStarted.Eagerly, 1)

    private suspend fun textChannel() = textChannelFlow().firstOrNull()

    private suspend fun sendDeath(serverEvent: PlayerDeathBEvent, channel: TextChannel) {
        val embed = EmbedBuilder()
            .setColor(0xb5123b)
            .setAuthor(
                serverEvent.cause ?: "${serverEvent.name} сдох =))",
                null,
                "https://mc-heads.net/avatar/${serverEvent.uuid}"
            )
            .build()
        channel.sendMessageEmbeds(embed).await()
    }

    private var lastOnlineChanged = System.currentTimeMillis().milliseconds
    private suspend fun changeOnlineCount(channel: TextChannel) {
        val current = System.currentTimeMillis().milliseconds
        if (current.minus(lastOnlineChanged) < 1.minutes) return
        lastOnlineChanged = current
        channel.manager.setTopic("Игроков в сети: ${onlinePlayersProvider.provide().size}").awaitWithTimeout(2.seconds)
    }

    private suspend fun sendJoined(serverEvent: PlayerJoinedBEvent, channel: TextChannel) {
        val text = when (serverEvent.hasPlayedBefore) {
            false -> "${serverEvent.name} присоединился впервые!"
            true -> "${serverEvent.name} присоединился"
        }
        val color = when (serverEvent.hasPlayedBefore) {
            false -> 0x34ebe5
            true -> 0x1fb82c
        }
        val embed = EmbedBuilder()
            .setColor(color)
            .setAuthor(
                text,
                null,
                "https://mc-heads.net/avatar/${serverEvent.uuid}"
            )
            .build()
        channel.sendMessageEmbeds(embed).await()
    }

    private suspend fun sendClosed(channel: TextChannel) {
        channel.manager.setTopic("Сервер только запустился...").awaitWithTimeout(2.seconds)
        channel.sendMessage("\uD83D\uDED1 **Сервер остановлен**").await()
    }

    private suspend fun sendOpen(channel: TextChannel) {
        channel.manager.setTopic("Сервер только запустился...").awaitWithTimeout(2.seconds)
        channel.sendMessage("✅ **Сервер успешно запущен**").await()
    }

    private suspend fun sendLeave(serverEvent: PlayerLeaveBEvent, channel: TextChannel) {
        val embed = EmbedBuilder()
            .setColor(0xb5123b)
            .setAuthor(
                "${serverEvent.name} покинул нас",
                null,
                "https://mc-heads.net/avatar/${serverEvent.uuid}"
            )
            .build()
        channel.sendMessageEmbeds(embed).await()
    }

    private suspend fun sendText(serverEvent: Text, channel: TextChannel) {
        val linkedPlayerModel = when (serverEvent) {
            is Text.Discord -> {
                linkingDao.findByDiscordId(serverEvent.authorId).getOrNull()
            }

            is Text.Minecraft -> {
                linkingDao.findByUuid(UUID.fromString(serverEvent.uuid)).getOrNull()
            }

            is Text.Telegram -> {
                linkingDao.findByTelegramId(serverEvent.authorId).getOrNull()
            }
        }

        val member = linkedPlayerModel?.discordLink
            ?.discordId
            ?.let(channel.guild::getMemberById)
            ?: linkedPlayerModel?.discordLink
                ?.discordId
                ?.let(channel.guild::retrieveMemberById)
                ?.await()

        // Change appearance of webhook message
        val message = WebhookMessageBuilder()
            .setUsername(
                when {
                    member != null -> "[${serverEvent.from.short}] ${member.effectiveName}"
                    else -> "[${serverEvent.from.short}] ${serverEvent.author}"
                }
            ) // use this username
            .setAvatarUrl(
                when (serverEvent) {
                    is Text.Discord -> error("Can't send discord to discord")
                    is Text.Minecraft -> {
                        member?.effectiveAvatarUrl
                            ?: "https://mc-heads.net/avatar/${serverEvent.uuid}"
                    }

                    is Text.Telegram -> {
                        member?.effectiveAvatarUrl
                            ?: "https://upload.wikimedia.org/wikipedia/commons/5/5c/Telegram_Messenger.png"
                    }
                }
            )
            .setContent(serverEvent.text.replace("@", ""))
            .build()

        val client = webHookClient()
        client.send(message)
    }

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.DISCORD) {
            return
        }
        val channel = textChannel() ?: run {
            error { "#onServerEvent could not get text channel" }
            return
        }

        when (bEvent) {
            is PlayerDeathBEvent -> {
                sendDeath(
                    channel = channel,
                    serverEvent = bEvent
                )
            }

            is PlayerJoinedBEvent -> {
                changeOnlineCount(channel)
                sendJoined(
                    channel = channel,
                    serverEvent = bEvent
                )
            }

            is PlayerLeaveBEvent -> {
                changeOnlineCount(channel)
                sendLeave(
                    channel = channel,
                    serverEvent = bEvent
                )
            }

            is Text -> {
                sendText(
                    bEvent,
                    channel
                )
            }

            ServerClosedBEvent -> {
                sendClosed(channel)
            }

            ServerOpenBEvent -> {
                sendOpen(channel)
            }
        }
    }

    init {
        BEventChannel
            .bEvents(this)
            .onEach { info { "#init receive event $it" } }
            .onEach { bEvent -> tryConsume(bEvent) }
            .launchIn(this)
    }
}
