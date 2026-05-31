package ru.astrainteractive.messagebridge.messenger.discord.messaging

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.awaitWithTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class DiscordTopicUpdater(
    private val onlinePlayersProvider: OnlinePlayersProvider,
) {
    private var lastOnlineChanged = System.currentTimeMillis().milliseconds

    suspend fun updateOnlineCount(channel: TextChannel) {
        val current = System.currentTimeMillis().milliseconds
        if (current.minus(lastOnlineChanged) < THROTTLE) return
        lastOnlineChanged = current
        channel.manager
            .setTopic("Игроков в сети: ${onlinePlayersProvider.provide().size}")
            .awaitWithTimeout(TOPIC_TIMEOUT)
    }

    suspend fun setStarting(channel: TextChannel) {
        channel.manager
            .setTopic("Сервер только запустился...")
            .awaitWithTimeout(TOPIC_TIMEOUT)
    }

    private companion object {
        val THROTTLE = 1.minutes
        val TOPIC_TIMEOUT = 2.seconds
    }
}
