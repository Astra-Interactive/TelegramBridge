package ru.astrainteractive.messagebridge.messenger.discord.relay.internal

import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.util.awaitRequest
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class DiscordTopicUpdater(
    private val onlinePlayersProvider: OnlinePlayersProvider,
    private val clock: Clock,
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val translation by translationKrate
    private val lastOnlineChange = AtomicReference(clock.now())

    private suspend fun setTopic(channel: TextChannel, topic: String): Result<Unit> {
        val result = withTimeoutOrNull(TOPIC_TIMEOUT) { awaitRequest { channel.manager.setTopic(topic) } }
        return result?.map { _ -> } ?: Result.success(Unit)
    }

    suspend fun updateOnlineCount(channel: TextChannel): Result<Unit> {
        val now = clock.now()
        val lastChange = lastOnlineChange.get()
        if (now - lastChange < THROTTLE || !lastOnlineChange.compareAndSet(lastChange, now)) return Result.success(Unit)
        val topic = translation.discord.chat.topicOnline(onlinePlayersProvider.provide().size)
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
