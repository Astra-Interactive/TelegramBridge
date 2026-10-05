@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.internal

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
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
    private val translatedTopicUpdater = topicUpdater(
        PluginTranslation(
            server = PluginTranslation.Server(discordTopicStarting = LocalizedText.shared("Booting")),
            onlinePlayers = PluginTranslation.OnlinePlayers(discordTopic = LocalizedText.shared("%count% online"))
        )
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

    private fun topicUpdater(translation: PluginTranslation): DiscordTopicUpdater {
        return DiscordTopicUpdater(
            platformServer = platformServer,
            clock = clock,
            translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
        )
    }

    @Test
    fun GIVEN_less_than_a_minute_since_start_WHEN_online_count_changes_THEN_topic_is_kept() {
        clock.current += 59.seconds

        translatedTopicUpdater.updateOnlineCount(textChannel)

        assertEquals(emptyList(), topics)
    }

    @Test
    fun GIVEN_a_minute_since_start_WHEN_online_count_changes_THEN_topic_reads_the_translated_online_count() {
        clock.current += 1.minutes

        translatedTopicUpdater.updateOnlineCount(textChannel)

        assertEquals(listOf("2 online"), topics)
    }

    @Test
    fun GIVEN_topic_just_updated_WHEN_online_count_changes_within_a_minute_THEN_the_change_waits() {
        clock.current += 1.minutes
        translatedTopicUpdater.updateOnlineCount(textChannel)
        clock.current += 59.seconds

        translatedTopicUpdater.updateOnlineCount(textChannel)

        assertEquals(1, topics.size)
    }

    @Test
    fun GIVEN_translated_texts_WHEN_server_starts_THEN_topic_reads_the_translated_starting_text() {
        translatedTopicUpdater.setStarting(textChannel)

        assertEquals(listOf("Booting"), topics)
    }

    @Test
    fun GIVEN_default_translation_WHEN_server_starts_and_online_count_changes_THEN_topics_read_the_english_texts() {
        val defaultTopicUpdater = topicUpdater(PluginTranslation())
        clock.current += 1.minutes

        defaultTopicUpdater.setStarting(textChannel)
        defaultTopicUpdater.updateOnlineCount(textChannel)

        assertEquals(listOf("The server has just started...", "Players online: 2"), topics)
    }
}
