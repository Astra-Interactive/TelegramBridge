package ru.astrainteractive.messagebridge.discord.event

import club.minnced.discord.webhook.send.WebhookMessageBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.messagebridge.discord.event.di.factory.WebHookClientFactory
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

class SocketEventListener(
    private val jda: JDA,
    private val serverBridgeApi: BridgeApi,
) : CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO) {
    private val webHookClient = WebHookClientFactory(jda)
        .create("756872937696526377")
        .shareIn(this, SharingStarted.Eagerly, 1)

    private suspend fun onServerEvent(event: ServerEvent) {
        if (event.from == ServerEvent.MessageFrom.DISCORD) {
            return
        }
        val channel = jda.getTextChannelById("756872937696526377") ?: run {
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
                channel.sendMessage("\uD83D\uDED1 **Сервер остановлен**").queue()
            }

            ServerEvent.ServerOpen -> {
                channel.sendMessage("✅ **Сервер успешно запущен**").queue()
            }
        }
    }

    init {
        serverBridgeApi.eventFlow()
            .onEach { onServerEvent(it) }
            .launchIn(CoroutineFeature.Default(Dispatchers.IO))
    }
}
