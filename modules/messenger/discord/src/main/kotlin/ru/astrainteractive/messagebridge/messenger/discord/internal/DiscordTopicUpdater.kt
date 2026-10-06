package ru.astrainteractive.messagebridge.messenger.discord.internal

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class DiscordTopicUpdater(
    private val platformServer: PlatformServer,
    private val clock: Clock,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-DiscordTopicUpdater") {
    private val translation by translationKrate
    private var lastOnlineChanged = clock.now()

    private fun setTopic(channel: TextChannel, topic: String) {
        runCatching {
            channel.manager
                .setTopic(topic)
                .timeout(TOPIC_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .queue(null) { t -> verbose { "#setTopic skipped the topic: ${t.message}" } }
        }.onFailure { t -> warn { "#setTopic could not set the topic: ${t.message}" } }
    }

    fun updateOnlineCount(channel: TextChannel) {
        val current = clock.now()
        if (current.minus(lastOnlineChanged) < THROTTLE) return
        lastOnlineChanged = current
        val onlineCount = platformServer.getOnlinePlayers().size
        setTopic(channel, translation.onlinePlayers.discordTopic(onlineCount).toMessengerText())
    }

    fun setStarting(channel: TextChannel) {
        setTopic(channel, translation.server.discordTopicStarting.toMessengerText())
    }

    private companion object {
        val THROTTLE = 1.minutes
        val TOPIC_TIMEOUT = 2.seconds
    }
}
