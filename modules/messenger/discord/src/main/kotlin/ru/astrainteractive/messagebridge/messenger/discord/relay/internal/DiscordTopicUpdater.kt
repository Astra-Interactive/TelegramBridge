package ru.astrainteractive.messagebridge.messenger.discord.relay.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.util.awaitRequest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class DiscordTopicUpdater(
    private val platformServer: PlatformServer,
    private val clock: Clock,
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val translation by translationKrate
    private val lastOnlineChangeMutex = Mutex()
    private var lastOnlineChange = clock.now()

    private suspend fun setTopic(channel: TextChannel, topic: String): Result<Unit> {
        val result = withTimeoutOrNull(TOPIC_TIMEOUT) { awaitRequest { channel.manager.setTopic(topic) } }
        return result?.map { _ -> } ?: Result.success(Unit)
    }

    suspend fun updateOnlineCount(channel: TextChannel): Result<Unit> {
        val isThrottled = lastOnlineChangeMutex.withLock {
            val now = clock.now()
            val isThrottled = now - lastOnlineChange < THROTTLE
            if (!isThrottled) lastOnlineChange = now
            isThrottled
        }
        if (isThrottled) return Result.success(Unit)
        val topic = translation.discord.chat.topicOnline(platformServer.getOnlinePlayers().size)
        return setTopic(channel, topic.toMessengerText())
    }

    suspend fun setStarting(channel: TextChannel): Result<Unit> {
        return setTopic(channel, translation.discord.chat.topicStarting.toMessengerText())
    }

    suspend fun setStopped(channel: TextChannel): Result<Unit> {
        return setTopic(channel, translation.discord.chat.topicStopped.toMessengerText())
    }

    private companion object {
        val THROTTLE = 1.minutes
        val TOPIC_TIMEOUT = 2.seconds
    }
}
