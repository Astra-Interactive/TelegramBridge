package ru.astrainteractive.messagebridge.messenger.discord.messaging

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

class DiscordMessageController(
    private val jdaFlow: Flow<JDA>,
    private val webHookClientFlow: Flow<WebhookClient>,
    kyoriKrate: Krate<KyoriComponentSerializer>,
    translationKrate: Krate<PluginTranslation>
) : MessageController, Logger by JUtiltLogger("MessageBridge-MinecraftMessageController") {
    private val kyori by kyoriKrate
    private val translation by translationKrate

    private suspend fun jda() = jdaFlow.first()
    private suspend fun webHookClient() = webHookClientFlow.first()

    override suspend fun send(serverEvent: ServerEvent) {
        if (serverEvent.from == ServerEvent.MessageFrom.DISCORD) {
            return
        }
        val channel = jda().getTextChannelById("756872937696526377") ?: run {
            error { "#onServerEvent could not get text channel" }
            return
        }

        when (serverEvent) {
            is ServerEvent.PlayerDeath -> {
                val embed = EmbedBuilder()
                    .setColor(0xb5123b)
                    .setAuthor(
                        serverEvent.cause ?: "${serverEvent.name} сдох =))",
                        null,
                        "https://mc-heads.net/avatar/${serverEvent.uuid}"
                    )
                    .build()
                channel.sendMessageEmbeds(embed).queue()
            }

            is ServerEvent.PlayerJoined -> {
                val embed = EmbedBuilder()
                    .setColor(0x1fb82c)
                    .setAuthor(
                        "${serverEvent.name} присоединился",
                        null,
                        "https://mc-heads.net/avatar/${serverEvent.uuid}"
                    )
                    .build()
                channel.sendMessageEmbeds(embed).queue()
            }

            is ServerEvent.PlayerLeave -> {
                val embed = EmbedBuilder()
                    .setColor(0xb5123b)
                    .setAuthor(
                        "${serverEvent.name} покинул нас",
                        null,
                        "https://mc-heads.net/avatar/${serverEvent.uuid}"
                    )
                    .build()
                channel.sendMessageEmbeds(embed).queue()
            }

            is ServerEvent.Text -> {
                // Change appearance of webhook message
                val message = WebhookMessageBuilder()
                    .setUsername("[${serverEvent.from.short}] ${serverEvent.author}") // use this username
                    .setAvatarUrl(
                        when (serverEvent) {
                            is ServerEvent.Text.Discord -> error("Can't send discord to discord")
                            is ServerEvent.Text.Minecraft -> "https://mc-heads.net/avatar/${serverEvent.uuid}"
                            is ServerEvent.Text.Telegram -> {
                                "https://upload.wikimedia.org/wikipedia/commons/5/5c/Telegram_Messenger.png"
                            }
                        }
                    )
                    .setContent(serverEvent.text.replace("@", ""))
                    .build()

                val client = webHookClient()
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
}
