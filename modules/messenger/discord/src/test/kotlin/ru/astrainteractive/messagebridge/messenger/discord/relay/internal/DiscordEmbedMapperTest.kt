@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.relay.internal

import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DiscordEmbedMapperTest {
    private val translation = PluginTranslation()
    private val mapper = DiscordEmbedMapper(
        translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
    )

    @Test
    fun GIVEN_death_with_message_of_the_game_WHEN_mapped_THEN_the_message_is_shown() {
        val embed = mapper.map(BEvent.PlayerDeath(name = "Steve", uuid = UUID, cause = "Steve was slain by Zombie"))

        assertEquals("Steve was slain by Zombie", embed.author?.name)
    }

    @Test
    fun GIVEN_death_without_message_WHEN_mapped_THEN_the_translation_names_the_player() {
        val embed = mapper.map(BEvent.PlayerDeath(name = "Steve", uuid = UUID, cause = null))

        assertEquals("Steve died", embed.author?.name)
    }

    @Test
    fun GIVEN_returning_player_WHEN_joined_THEN_it_differs_from_the_first_join() {
        val returning = mapper.map(BEvent.PlayerJoined(name = "Steve", uuid = UUID, hasPlayedBefore = true))
        val newcomer = mapper.map(BEvent.PlayerJoined(name = "Steve", uuid = UUID, hasPlayedBefore = false))

        assertEquals("Steve joined the server", returning.author?.name)
        assertEquals("Steve joined the server for the first time!", newcomer.author?.name)
        assertNotEquals(returning.colorRaw, newcomer.colorRaw)
    }

    @Test
    fun GIVEN_player_WHEN_left_THEN_the_head_of_the_skin_is_shown() {
        val embed = mapper.map(BEvent.PlayerLeave(name = "Steve", uuid = UUID))

        assertEquals("Steve left the server", embed.author?.name)
        assertEquals("https://mc-heads.net/avatar/$UUID", embed.author?.iconUrl)
    }

    @Test
    fun GIVEN_name_with_markup_WHEN_mapped_THEN_markup_is_kept_as_text() {
        val embed = mapper.map(BEvent.PlayerLeave(name = "<red>&cSteve", uuid = UUID))

        assertEquals("<red>&cSteve left the server", embed.author?.name)
    }

    private companion object {
        const val UUID = "069a79f4-44e9-4726-a5be-fca90e38aaf5"
    }
}
