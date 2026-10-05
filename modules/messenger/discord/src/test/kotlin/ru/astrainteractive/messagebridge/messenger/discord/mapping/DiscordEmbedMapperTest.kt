@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.mapping

import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerLeaveBEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordEmbedMapperTest {
    private val translatedMapper = mapper(
        PluginTranslation(
            player = PluginTranslation.Player(
                discordJoined = LocalizedText.shared("%dao% is back"),
                discordJoinedFirstTime = LocalizedText.shared("%dao% is new here"),
                discordLeft = LocalizedText.shared("%dao% is gone"),
                discordDiedOfUnknownCause = LocalizedText.shared("%dao% just died")
            )
        )
    )
    private val defaultMapper = mapper(PluginTranslation())
    private val returningSteve = PlayerJoinedBEvent(name = "Steve", uuid = STEVE_UUID, hasPlayedBefore = true)
    private val newSteve = returningSteve.copy(hasPlayedBefore = false)
    private val leavingSteve = PlayerLeaveBEvent(name = "Steve", uuid = STEVE_UUID)
    private val deadSteve = PlayerDeathBEvent(name = "Steve", cause = null, uuid = STEVE_UUID)

    private fun mapper(translation: PluginTranslation): DiscordEmbedMapper {
        return DiscordEmbedMapper(DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate())
    }

    @Test
    fun GIVEN_translated_texts_WHEN_returning_player_joins_THEN_embed_reads_the_join_text() {
        assertEquals("Steve is back", translatedMapper.map(returningSteve).author?.name)
    }

    @Test
    fun GIVEN_translated_texts_WHEN_new_player_joins_THEN_embed_reads_the_first_join_text() {
        assertEquals("Steve is new here", translatedMapper.map(newSteve).author?.name)
    }

    @Test
    fun GIVEN_translated_texts_WHEN_player_leaves_THEN_embed_reads_the_leave_text() {
        assertEquals("Steve is gone", translatedMapper.map(leavingSteve).author?.name)
    }

    @Test
    fun GIVEN_translated_texts_WHEN_player_dies_of_unknown_cause_THEN_embed_reads_the_death_text() {
        assertEquals("Steve just died", translatedMapper.map(deadSteve).author?.name)
    }

    @Test
    fun GIVEN_translated_texts_WHEN_player_dies_of_known_cause_THEN_embed_reads_the_game_death_message() {
        val death = deadSteve.copy(cause = "Steve tried to swim in lava")

        assertEquals("Steve tried to swim in lava", translatedMapper.map(death).author?.name)
    }

    @Test
    fun GIVEN_default_translation_WHEN_player_events_are_mapped_THEN_embeds_read_the_english_texts() {
        val authorNames = listOf(
            defaultMapper.map(returningSteve),
            defaultMapper.map(newSteve),
            defaultMapper.map(leavingSteve),
            defaultMapper.map(deadSteve)
        ).map { embed -> embed.author?.name }

        assertEquals(
            listOf("Steve joined", "Steve joined for the first time!", "Steve left", "Steve died =))"),
            authorNames
        )
    }

    private companion object {
        const val STEVE_UUID = "8667ba71-b85a-4004-af54-457a9734eed7"
    }
}
