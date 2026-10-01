package ru.astrainteractive.messagebridge.messenger.discord.messaging

import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Discord allows two topic changes in ten minutes, so the message is not held back by an update that waits. */
internal class DiscordTopicUpdater(
    private val onlinePlayersProvider: OnlinePlayersProvider,
) {
    private var lastOnlineChanged = System.currentTimeMillis().milliseconds

    suspend fun updateOnlineCount(channel: TextChannel) {
        val current = System.currentTimeMillis().milliseconds
        if (current.minus(lastOnlineChanged) < THROTTLE) return
        lastOnlineChanged = current
        withTimeoutOrNull(TOPIC_TIMEOUT) {
            channel.manager
                .setTopic("Игроков в сети: ${onlinePlayersProvider.provide().size}")
                .await()
        }
    }

    suspend fun setStarting(channel: TextChannel) {
        withTimeoutOrNull(TOPIC_TIMEOUT) {
            channel.manager
                .setTopic("Сервер только запустился...")
                .await()
        }
    }

    private companion object {
        val THROTTLE = 1.minutes
        val TOPIC_TIMEOUT = 2.seconds
    }
}
