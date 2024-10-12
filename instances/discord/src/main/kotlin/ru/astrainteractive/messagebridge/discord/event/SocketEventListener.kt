package ru.astrainteractive.messagebridge.discord.event

import club.minnced.discord.webhook.send.WebhookMessageBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.data.OnlineListMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.ServerEventMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.UpdateOnlineMessageData
import ru.astrainteractive.messagebridge.discord.event.di.factory.WebHookClientFactory
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

class SocketEventListener(
    private val jda: JDA,
    serverBridgeApi: BridgeApi,
) : CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO),
    Logger by JUtiltLogger("SocketEventListener") {
    private val webHookClient = WebHookClientFactory(jda)
        .create("756872937696526377")
        .shareIn(this, SharingStarted.Lazily, 1)

    @Suppress("LongMethod")
    private suspend fun onServerEvent(event: ServerEvent) {
        if (event.from == ServerEvent.MessageFrom.DISCORD) {
            return
        }
        val channel = jda.getTextChannelById("756872937696526377") ?: run {
            error { "#onServerEvent could not get text channel" }
            return
        }

        when (event) {
            is ServerEvent.PlayerDeath -> {
                val embed = EmbedBuilder()
                    .setColor(0xb5123b)
                    .setAuthor(
                        event.cause ?: "${event.name} сдох =))",
                        null,
                        "https://mc-heads.net/avatar/${event.uuid}"
                    )
                    .build()
                channel.sendMessageEmbeds(embed).queue()
            }

            is ServerEvent.PlayerJoined -> {
                val embed = EmbedBuilder()
                    .setColor(0x1fb82c)
                    .setAuthor(
                        "${event.name} присоединился",
                        null,
                        "https://mc-heads.net/avatar/${event.uuid}"
                    )
                    .build()
                channel.sendMessageEmbeds(embed).queue()
            }

            is ServerEvent.PlayerLeave -> {
                val embed = EmbedBuilder()
                    .setColor(0xb5123b)
                    .setAuthor(
                        "${event.name} покинул нас",
                        null,
                        "https://mc-heads.net/avatar/${event.uuid}"
                    )
                    .build()
                channel.sendMessageEmbeds(embed).queue()
            }

            is ServerEvent.Text -> {
                // Change appearance of webhook message
                val message = WebhookMessageBuilder()
                    .setUsername("[${event.from.short}] ${event.author}") // use this username
                    .setAvatarUrl(
                        when (event) {
                            is ServerEvent.Text.Discord -> error("Can't send discord to discord")
                            is ServerEvent.Text.Minecraft -> "https://mc-heads.net/avatar/${event.uuid}"
                            is ServerEvent.Text.Telegram -> {
                                "https://upload.wikimedia.org/wikipedia/commons/5/5c/Telegram_Messenger.png"
                            }
                        }
                    )
                    .setContent(event.text.replace("@", ""))
                    .build()

                val client = webHookClient.first()
                client.send(message)
            }

            ServerEvent.ServerClosed -> {
                channel.manager.setTopic("Рестриминг чата из игры")
                channel.sendMessage("\uD83D\uDED1 **Сервер остановлен**").queue()
            }

            ServerEvent.ServerOpen -> {
                channel.manager.setTopic("Сервер только запустился...")
                channel.sendMessage("✅ **Сервер успешно запущен**").queue()
            }
        }
    }

    private fun onOnlineUpdate(data: UpdateOnlineMessageData) {
        val channel = jda.getTextChannelById("756872937696526377") ?: run {
            error { "#onServerEvent could not get text channel" }
            return
        }
        channel.manager.setTopic("Сейчас онлайн: ${data.current}/${data.max}")
    }

    private fun onOnlineList(data: OnlineListMessageData) {
        info { "#onOnlineList" }
        val channel = jda.getTextChannelById("756872937696526377") ?: run {
            error { "#onServerEvent could not get text channel" }
            return
        }
        val text = data.onlinePlayers.joinToString(
            separator = ", ",
            prefix = "Сейчас онлайн ${data.onlinePlayers.size} игроков:\n"
        )
        channel.sendMessage(text).queue()
    }

    init {
        serverBridgeApi.eventFlow()
            .filterIsInstance<ServerEventMessageData>()
            .onEach { onServerEvent(it.instance) }
            .launchIn(this)

        serverBridgeApi.eventFlow()
            .filterIsInstance<UpdateOnlineMessageData>()
            .onEach { onOnlineUpdate(it) }
            .launchIn(this)

        serverBridgeApi.eventFlow()
            .filterIsInstance<OnlineListMessageData>()
            .onEach { onOnlineList(it) }
            .launchIn(this)
    }
}
