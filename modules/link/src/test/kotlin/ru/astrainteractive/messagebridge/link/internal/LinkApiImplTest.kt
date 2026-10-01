@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.objects.User
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.link.code.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.code.model.CodeUser
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.link.model.UnlinkResponse
import ru.astrainteractive.messagebridge.link.player.fake.FakeDiscordLinkedPlayerDao
import ru.astrainteractive.messagebridge.link.player.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.role.fake.FakePermissionGroups
import ru.astrainteractive.messagebridge.link.role.internal.DiscordLinkRole
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class LinkApiImplTest {
    private val link = PluginConfiguration.Link(linkDiscordRole = "1", linkLuckPermsRole = GROUP)
    private val config = MutableStateFlow(PluginConfiguration(link = link))
    private val discordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = "steve", discordId = DISCORD_ID)
    private val telegramLink = LinkedPlayerModel.TelegramLink(telegramUsername = "steve_tg", telegramId = TELEGRAM_ID)
    private val steve = LinkedPlayerModel(
        uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"),
        lastMinecraftName = "Steve",
        discordLink = discordLink,
        telegramLink = null
    )
    private val groups = FakePermissionGroups()
    private val codeApi = CodeApiImpl(Random(SEED))

    private fun createLinkApi(linkingDao: FakeLinkingDao): LinkApiImpl {
        return LinkApiImpl(
            linkingDao = linkingDao,
            discordLinkedPlayerDao = FakeDiscordLinkedPlayerDao(linkingDao),
            codeApi = codeApi,
            discordLinkRole = DiscordLinkRole(),
            permissionGroups = groups,
            configFlow = config
        )
    }

    private suspend fun giveGroupTo(player: LinkedPlayerModel) {
        groups.add(player.uuid, GROUP)
    }

    @Test
    fun GIVEN_player_linked_only_on_discord_WHEN_member_left_THEN_group_is_taken_and_link_is_gone() = runTest {
        val linkingDao = FakeLinkingDao(steve)
        giveGroupTo(steve)

        createLinkApi(linkingDao).revokeLeftMember(DISCORD_ID)

        assertEquals(emptySet<String>(), groups.groupsOf(steve.uuid))
        assertNull(linkingDao.players[steve.uuid])
    }

    @Test
    fun GIVEN_player_linked_on_telegram_too_WHEN_member_left_THEN_telegram_keeps_the_link_and_the_group() = runTest {
        val linkingDao = FakeLinkingDao(steve.copy(telegramLink = telegramLink))
        giveGroupTo(steve)

        createLinkApi(linkingDao).revokeLeftMember(DISCORD_ID)

        assertEquals(setOf(GROUP), groups.groupsOf(steve.uuid))
        assertEquals(steve.copy(discordLink = null, telegramLink = telegramLink), linkingDao.players[steve.uuid])
    }

    @Test
    fun GIVEN_linking_gives_no_roles_WHEN_member_left_THEN_nothing_changes() = runTest {
        config.value = PluginConfiguration(link = null)
        val linkingDao = FakeLinkingDao(steve)
        giveGroupTo(steve)

        createLinkApi(linkingDao).revokeLeftMember(DISCORD_ID)

        assertEquals(setOf(GROUP), groups.groupsOf(steve.uuid))
        assertEquals(steve, linkingDao.players[steve.uuid])
    }

    @Test
    fun GIVEN_discord_account_nobody_linked_WHEN_member_left_THEN_nothing_changes() = runTest {
        val linkingDao = FakeLinkingDao(steve)
        giveGroupTo(steve)

        createLinkApi(linkingDao).revokeLeftMember(OTHER_DISCORD_ID)

        assertEquals(setOf(GROUP), groups.groupsOf(steve.uuid))
        assertEquals(steve, linkingDao.players[steve.uuid])
    }

    @Test
    fun GIVEN_group_cannot_be_taken_WHEN_member_left_THEN_link_stays_to_be_tried_again() = runTest {
        val linkingDao = FakeLinkingDao(steve)
        groups.failure = IllegalStateException("LuckPerms is not installed")

        createLinkApi(linkingDao).revokeLeftMember(DISCORD_ID)

        assertEquals(steve, linkingDao.players[steve.uuid])
    }

    @Test
    fun GIVEN_links_cannot_be_read_WHEN_member_left_THEN_the_group_is_kept() = runTest {
        val linkingDao = FakeLinkingDao(steve)
        giveGroupTo(steve)
        linkingDao.failure = IllegalStateException("database is closed")

        createLinkApi(linkingDao).revokeLeftMember(DISCORD_ID)

        assertEquals(setOf(GROUP), groups.groupsOf(steve.uuid))
    }

    @Test
    fun GIVEN_member_leaves_while_linking_telegram_WHEN_both_finish_THEN_telegram_link_and_group_stay() = runTest {
        val linkingDao = FakeLinkingDao(steve)
        giveGroupTo(steve)
        val linkApi = createLinkApi(linkingDao)
        val code = codeApi.generateCodeForPlayer(CodeUser(name = steve.lastMinecraftName, uuid = steve.uuid))
        val telegramUser = User(TELEGRAM_ID, "Steve", false).apply { userName = "steve_tg" }

        listOf(
            launch { linkApi.revokeLeftMember(DISCORD_ID) },
            launch { assertIs<LinkResponse.Linked>(linkApi.linkTelegram(code, telegramUser)) }
        ).joinAll()

        val storedPlayer = linkingDao.players[steve.uuid]
        assertNull(storedPlayer?.discordLink)
        assertEquals(telegramLink, storedPlayer?.telegramLink)
        assertEquals(setOf(GROUP), groups.groupsOf(steve.uuid))
    }

    @Test
    fun GIVEN_member_left_while_the_server_was_off_WHEN_members_are_compared_THEN_only_they_are_revoked() = runTest {
        val alex = steve.copy(
            uuid = UUID.fromString("ec561538-f3fd-461d-aff5-086b22154bce"),
            lastMinecraftName = "Alex",
            discordLink = discordLink.copy(discordId = OTHER_DISCORD_ID)
        )
        val linkingDao = FakeLinkingDao(steve, alex)
        giveGroupTo(steve)
        giveGroupTo(alex)

        createLinkApi(linkingDao).revokeAbsentMembers(memberIds = setOf(OTHER_DISCORD_ID, BOT_ID))

        assertNull(linkingDao.players[steve.uuid])
        assertEquals(emptySet<String>(), groups.groupsOf(steve.uuid))
        assertEquals(alex, linkingDao.players[alex.uuid])
        assertEquals(setOf(GROUP), groups.groupsOf(alex.uuid))
    }

    @Test
    fun GIVEN_every_linked_member_is_still_there_WHEN_members_are_compared_THEN_nothing_changes() = runTest {
        val linkingDao = FakeLinkingDao(steve)
        giveGroupTo(steve)

        createLinkApi(linkingDao).revokeAbsentMembers(memberIds = setOf(DISCORD_ID, BOT_ID))

        assertEquals(steve, linkingDao.players[steve.uuid])
        assertEquals(setOf(GROUP), groups.groupsOf(steve.uuid))
    }

    @Test
    fun GIVEN_linking_gives_no_roles_WHEN_members_are_compared_THEN_nothing_changes() = runTest {
        config.value = PluginConfiguration(link = null)
        val linkingDao = FakeLinkingDao(steve)

        createLinkApi(linkingDao).revokeAbsentMembers(memberIds = setOf(BOT_ID))

        assertEquals(steve, linkingDao.players[steve.uuid])
    }

    @Test
    fun GIVEN_no_members_were_loaded_WHEN_members_are_compared_THEN_nobody_is_revoked() = runTest {
        val linkingDao = FakeLinkingDao(steve)
        giveGroupTo(steve)

        createLinkApi(linkingDao).revokeAbsentMembers(memberIds = emptySet())

        assertEquals(steve, linkingDao.players[steve.uuid])
        assertEquals(setOf(GROUP), groups.groupsOf(steve.uuid))
    }

    @Test
    fun GIVEN_linked_players_cannot_be_read_WHEN_members_are_compared_THEN_nobody_is_revoked() = runTest {
        val linkingDao = FakeLinkingDao(steve)
        giveGroupTo(steve)
        linkingDao.failure = IllegalStateException("database is closed")

        createLinkApi(linkingDao).revokeAbsentMembers(memberIds = setOf(BOT_ID))

        assertEquals(setOf(GROUP), groups.groupsOf(steve.uuid))
    }

    @Test
    fun GIVEN_member_left_during_the_comparison_WHEN_both_finish_THEN_the_link_is_cleared_once() = runTest {
        val linkingDao = FakeLinkingDao(steve.copy(telegramLink = telegramLink))
        giveGroupTo(steve)
        val linkApi = createLinkApi(linkingDao)

        listOf(
            launch { linkApi.revokeAbsentMembers(memberIds = setOf(BOT_ID)) },
            launch { linkApi.revokeLeftMember(DISCORD_ID) }
        ).joinAll()

        assertEquals(steve.copy(discordLink = null, telegramLink = telegramLink), linkingDao.players[steve.uuid])
        assertEquals(setOf(GROUP), groups.groupsOf(steve.uuid))
    }

    @Test
    fun GIVEN_linked_player_WHEN_unlinked_THEN_group_is_taken_and_every_link_is_gone() = runTest {
        val linkingDao = FakeLinkingDao(steve.copy(telegramLink = telegramLink))
        giveGroupTo(steve)

        val response = createLinkApi(linkingDao).unlink(steve.uuid)

        assertEquals(UnlinkResponse.Unlinked, response)
        assertEquals(emptySet<String>(), groups.groupsOf(steve.uuid))
        assertNull(linkingDao.players[steve.uuid])
    }

    @Test
    fun GIVEN_linking_gives_no_roles_WHEN_unlinked_THEN_links_are_gone_and_groups_are_untouched() = runTest {
        config.value = PluginConfiguration(link = null)
        val linkingDao = FakeLinkingDao(steve)
        giveGroupTo(steve)

        val response = createLinkApi(linkingDao).unlink(steve.uuid)

        assertEquals(UnlinkResponse.Unlinked, response)
        assertEquals(setOf(GROUP), groups.groupsOf(steve.uuid))
        assertNull(linkingDao.players[steve.uuid])
    }

    @Test
    fun GIVEN_player_without_links_WHEN_unlinked_THEN_is_not_linked() = runTest {
        val response = createLinkApi(FakeLinkingDao()).unlink(steve.uuid)

        assertEquals(UnlinkResponse.NotLinked, response)
    }

    @Test
    fun GIVEN_group_cannot_be_taken_WHEN_unlinked_THEN_link_stays_to_be_tried_again() = runTest {
        val linkingDao = FakeLinkingDao(steve)
        groups.failure = IllegalStateException("LuckPerms is not installed")

        val response = createLinkApi(linkingDao).unlink(steve.uuid)

        assertEquals(UnlinkResponse.UnknownError, response)
        assertEquals(steve, linkingDao.players[steve.uuid])
    }

    @Test
    fun GIVEN_links_cannot_be_read_WHEN_unlinked_THEN_group_is_kept() = runTest {
        val linkingDao = FakeLinkingDao(steve)
        giveGroupTo(steve)
        linkingDao.failure = IllegalStateException("database is closed")

        val response = createLinkApi(linkingDao).unlink(steve.uuid)

        assertEquals(UnlinkResponse.UnknownError, response)
        assertEquals(setOf(GROUP), groups.groupsOf(steve.uuid))
    }

    private companion object {
        const val GROUP = "verified"
        const val DISCORD_ID = 42L
        const val OTHER_DISCORD_ID = 43L
        const val TELEGRAM_ID = 7L
        const val BOT_ID = 100L
        const val SEED = 1
    }
}
