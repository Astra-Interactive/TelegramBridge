@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.internal

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.messagebridge.messenger.discord.fake.FakeClock
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DiscordTopicUpdaterTest {
    private val topics = mutableListOf<String>()
    private val clock = FakeClock(Instant.fromEpochSeconds(0))
    private val onlinePlayers = listOf(jdaFake<OnlineKPlayer>(emptyMap()), jdaFake<OnlineKPlayer>(emptyMap()))
    private val platformServer: PlatformServer = jdaFake(mapOf("getOnlinePlayers" to onlinePlayers))
    private val textChannel: TextChannel = jdaFake(mapOf("getManager" to recordingTopicManager()))
    private val topicUpdater = DiscordTopicUpdater(
        platformServer = platformServer,
        clock = clock
    )

    private fun recordingTopicManager(): TextChannelManager {
        lateinit var manager: TextChannelManager
        manager = jdaFake(
            mapOf(
                "setTopic" to JdaAnswer { args ->
                    topics += args.first().toString()
                    manager
                },
                "timeout" to JdaAnswer { _ -> manager },
                "queue" to null
            )
        )
        return manager
    }

    @Test
    fun GIVEN_less_than_a_minute_since_start_WHEN_online_count_changes_THEN_topic_is_kept() {
        clock.current += 59.seconds

        topicUpdater.updateOnlineCount(textChannel)

        assertEquals(emptyList(), topics)
    }

    @Test
    fun GIVEN_a_minute_since_start_WHEN_online_count_changes_THEN_topic_shows_the_online_count() {
        clock.current += 1.minutes

        topicUpdater.updateOnlineCount(textChannel)

        assertEquals(listOf("Игроков в сети: 2"), topics)
    }

    @Test
    fun GIVEN_topic_just_updated_WHEN_online_count_changes_within_a_minute_THEN_the_change_waits() {
        clock.current += 1.minutes
        topicUpdater.updateOnlineCount(textChannel)
        clock.current += 59.seconds

        topicUpdater.updateOnlineCount(textChannel)

        assertEquals(1, topics.size)
    }
}
