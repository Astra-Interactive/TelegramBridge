@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.discord.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.exceptions.HierarchyException
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messenger.discord.fake.FakeWebhookClient
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRoleChange
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class DiscordRoleUpdaterTest {
    private val appliedChanges = mutableListOf<String>()
    private val usersWhoseRequestJdaCancels = mutableSetOf<Long>()
    private var botCanManageRoles = true
    private val linkedRole: Role = jdaFake(mapOf("getIdLong" to ROLE_ID))
    private val steveGrant = DiscordRoleChange.Grant(discordUserId = STEVE_ID, roleId = ROLE_ID)
    private val guild: Guild = jdaFake(
        mapOf(
            "getRoleById" to JdaAnswer { args -> linkedRole.takeIf { _ -> args.first() == ROLE_ID } },
            "addRoleToMember" to JdaAnswer { args ->
                if (!botCanManageRoles) {
                    throw HierarchyException("Can't modify a role with higher or equal highest role than yourself!")
                }
                roleRequest(change = "grant", args = args)
            },
            "removeRoleFromMember" to JdaAnswer { args -> roleRequest(change = "revoke", args = args) }
        )
    )

    private val ready = DiscordChannel.Ready(
        textChannel = jdaFake<TextChannel>(mapOf("getGuild" to guild)),
        webhookClient = FakeWebhookClient()
    )

    private fun roleRequest(change: String, args: List<Any?>): AuditableRestAction<Void> {
        val userId = args.first()
            ?.tryCast<UserSnowflake>()
            ?.idLong
            ?: error("$change got no user")
        val roleId = args[1]
            ?.tryCast<Role>()
            ?.idLong
        return jdaFake(
            mapOf(
                "queue" to JdaAnswer { queueArgs ->
                    if (userId in usersWhoseRequestJdaCancels) {
                        queueArgs[1]
                            ?.tryCast<Consumer<Throwable>>()
                            ?.accept(CancellationException("RestAction has been cancelled"))
                    } else {
                        appliedChanges += "$change:$userId:$roleId"
                        queueArgs.first()
                            ?.tryCast<Consumer<Void?>>()
                            ?.accept(null)
                    }
                }
            )
        )
    }

    private fun updater(
        discordChannel: Flow<DiscordChannel>,
        roleChanges: Flow<DiscordRoleChange> = emptyFlow()
    ): DiscordRoleUpdater {
        return DiscordRoleUpdater(discordChannel = discordChannel, roleChanges = roleChanges)
    }

    @Test
    fun GIVEN_ready_discord_WHEN_grant_is_updated_THEN_the_user_gets_the_role() = runTest {
        updater(MutableStateFlow(ready)).update(steveGrant)

        assertEquals(listOf("grant:$STEVE_ID:$ROLE_ID"), appliedChanges)
    }

    @Test
    fun GIVEN_ready_discord_WHEN_revoke_is_updated_THEN_the_role_is_removed_from_the_user() = runTest {
        val steveRevoke = DiscordRoleChange.Revoke(discordUserId = STEVE_ID, roleId = ROLE_ID)

        updater(MutableStateFlow(ready)).update(steveRevoke)

        assertEquals(listOf("revoke:$STEVE_ID:$ROLE_ID"), appliedChanges)
    }

    @Test
    fun GIVEN_role_missing_on_the_server_WHEN_grant_is_updated_THEN_no_role_is_changed() = runTest {
        val unknownRoleGrant = DiscordRoleChange.Grant(discordUserId = STEVE_ID, roleId = UNKNOWN_ROLE_ID)

        updater(MutableStateFlow(ready)).update(unknownRoleGrant)

        assertTrue(appliedChanges.isEmpty())
    }

    @Test
    fun GIVEN_bot_that_can_not_manage_the_role_WHEN_grant_is_updated_THEN_the_failure_does_not_escape() = runTest {
        botCanManageRoles = false

        updater(MutableStateFlow(ready)).update(steveGrant)

        assertTrue(appliedChanges.isEmpty())
    }

    @Test
    fun GIVEN_discord_failed_WHEN_it_reconnects_ten_minutes_later_THEN_the_change_is_applied() = runTest {
        val state = MutableStateFlow<DiscordChannel>(DiscordChannel.Failed)
        val update = launch { updater(state).update(steveGrant) }
        advanceTimeBy(5.minutes)
        state.value = DiscordChannel.Connecting
        advanceTimeBy(5.minutes)

        state.value = ready
        runCurrent()

        assertTrue(update.isCompleted)
        assertEquals(listOf("grant:$STEVE_ID:$ROLE_ID"), appliedChanges)
    }

    @Test
    fun GIVEN_discord_not_configured_WHEN_grant_is_updated_THEN_it_is_skipped() = runTest {
        updater(MutableStateFlow(DiscordChannel.Disabled)).update(steveGrant)

        assertTrue(appliedChanges.isEmpty())
    }

    @Test
    fun GIVEN_discord_connecting_WHEN_it_becomes_not_configured_THEN_the_change_is_skipped() = runTest {
        val state = MutableStateFlow<DiscordChannel>(DiscordChannel.Connecting)
        val update = launch { updater(state).update(steveGrant) }
        runCurrent()

        state.value = DiscordChannel.Disabled
        runCurrent()

        assertTrue(update.isCompleted)
        assertTrue(appliedChanges.isEmpty())
    }

    @Test
    fun GIVEN_discord_cancels_a_sent_change_WHEN_the_next_change_arrives_THEN_it_is_applied() = runTest {
        usersWhoseRequestJdaCancels += ALEX_ID
        val alexGrant = DiscordRoleChange.Grant(discordUserId = ALEX_ID, roleId = ROLE_ID)
        val updater = updater(
            discordChannel = MutableStateFlow(ready),
            roleChanges = flowOf(alexGrant, steveGrant)
        )

        updater.coroutineContext.job.children.toList().joinAll()

        assertEquals(listOf("grant:$STEVE_ID:$ROLE_ID"), appliedChanges)
    }

    private companion object {
        const val STEVE_ID = 4242L
        const val ALEX_ID = 4343L
        const val ROLE_ID = 123456789012345678L
        const val UNKNOWN_ROLE_ID = 987654321098765432L
    }
}
