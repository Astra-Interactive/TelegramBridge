@file:Suppress("FunctionNaming", "ExpressionBodyOnOneLine")

package ru.astrainteractive.messagebridge.integration.luckperms.role

import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.model.PluginConfiguration
import ru.astrainteractive.messagebridge.integration.luckperms.fake.FakeLuckPerms
import ru.astrainteractive.messagebridge.integration.luckperms.fake.FakeLuckPermsProvider
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class LuckPermsLinkRoleRepositoryTest {
    private val steve = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002")

    private fun linkConfiguration(): PluginConfiguration = PluginConfiguration(
        link = PluginConfiguration.Link(linkDiscordRole = "123456789012345678", linkLuckPermsRole = LINKED_GROUP)
    )

    private fun repository(
        luckPermsProvider: FakeLuckPermsProvider,
        configuration: PluginConfiguration = linkConfiguration()
    ): LuckPermsLinkRoleRepository = LuckPermsLinkRoleRepository(
        configKrate = DefaultMutableKrate(factory = { configuration }, loader = { null }).asCachedKrate(),
        luckPermsProvider = luckPermsProvider
    )

    @Test
    fun GIVEN_player_without_the_group_WHEN_link_role_is_added_THEN_player_gets_the_configured_group() {
        val luckPerms = FakeLuckPerms(saveFailure = null)

        repository(FakeLuckPermsProvider(luckPerms.luckPerms)).addLinkRole(steve)

        assertEquals(setOf(LINKED_GROUP), luckPerms.groupsOf(steve))
    }

    @Test
    fun GIVEN_player_with_the_group_WHEN_link_role_is_added_THEN_the_group_is_not_added_again() {
        val luckPerms = FakeLuckPerms(saveFailure = null)
        luckPerms.addGroup(steve, LINKED_GROUP)

        repository(FakeLuckPermsProvider(luckPerms.luckPerms)).addLinkRole(steve)

        assertEquals(0, luckPerms.mutationCount)
    }

    @Test
    fun GIVEN_player_with_the_group_WHEN_link_role_is_removed_THEN_player_loses_the_group() {
        val luckPerms = FakeLuckPerms(saveFailure = null)
        luckPerms.addGroup(steve, LINKED_GROUP)
        luckPerms.addGroup(steve, OTHER_GROUP)

        repository(FakeLuckPermsProvider(luckPerms.luckPerms)).removeLinkedRole(steve)

        assertEquals(setOf(OTHER_GROUP), luckPerms.groupsOf(steve))
    }

    @Test
    fun GIVEN_player_without_the_group_WHEN_link_role_is_removed_THEN_nothing_changes() {
        val luckPerms = FakeLuckPerms(saveFailure = null)

        repository(FakeLuckPermsProvider(luckPerms.luckPerms)).removeLinkedRole(steve)

        assertEquals(0, luckPerms.mutationCount)
    }

    @Test
    fun GIVEN_luckperms_that_can_not_save_WHEN_link_role_is_added_or_removed_THEN_nothing_is_thrown() {
        val luckPerms = FakeLuckPerms(saveFailure = IllegalStateException("Storage is read-only"))
        val repository = repository(FakeLuckPermsProvider(luckPerms.luckPerms))

        repository.addLinkRole(steve)
        repository.removeLinkedRole(steve)

        assertEquals(2, luckPerms.mutationCount)
    }

    @Test
    fun GIVEN_no_luckperms_WHEN_link_role_is_added_or_removed_THEN_nothing_is_thrown() {
        val luckPermsProvider = FakeLuckPermsProvider(luckPerms = null)
        val repository = repository(luckPermsProvider)

        repository.addLinkRole(steve)
        repository.removeLinkedRole(steve)

        assertEquals(2, luckPermsProvider.provideCallCount)
    }

    @Test
    fun GIVEN_no_link_settings_WHEN_link_role_is_added_or_removed_THEN_luckperms_is_not_asked() {
        val luckPermsProvider = FakeLuckPermsProvider(FakeLuckPerms(saveFailure = null).luckPerms)
        val repository = repository(luckPermsProvider, PluginConfiguration(link = null))

        repository.addLinkRole(steve)
        repository.removeLinkedRole(steve)

        assertEquals(0, luckPermsProvider.provideCallCount)
    }

    private companion object {
        const val LINKED_GROUP = "verified"
        const val OTHER_GROUP = "vip"
    }
}
