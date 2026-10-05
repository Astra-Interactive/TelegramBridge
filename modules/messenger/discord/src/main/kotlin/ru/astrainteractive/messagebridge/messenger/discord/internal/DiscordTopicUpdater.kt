package ru.astrainteractive.messagebridge.messenger.discord.internal

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class DiscordTopicUpdater(
    private val platformServer: PlatformServer,
) : Logger by JUtiltLogger("MessageBridge-DiscordTopicUpdater") {
    private var lastOnlineChanged = System.currentTimeMillis().milliseconds

    private fun setTopic(channel: TextChannel, topic: String) {
        runCatching {
            channel.manager
                .setTopic(topic)
                .timeout(TOPIC_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .queue(null) { t -> verbose { "#setTopic skipped the topic: ${t.message}" } }
        }.onFailure { t -> warn { "#setTopic could not set the topic: ${t.message}" } }
    }

    fun updateOnlineCount(channel: TextChannel) {
        val current = System.currentTimeMillis().milliseconds
        if (current.minus(lastOnlineChanged) < THROTTLE) return
        lastOnlineChanged = current
        setTopic(channel, "Игроков в сети: ${platformServer.getOnlinePlayers().size}")
    }

    fun setStarting(channel: TextChannel) {
        setTopic(channel, "Сервер только запустился...")
    }

    private companion object {
        val THROTTLE = 1.minutes
        val TOPIC_TIMEOUT = 2.seconds
    }
}
