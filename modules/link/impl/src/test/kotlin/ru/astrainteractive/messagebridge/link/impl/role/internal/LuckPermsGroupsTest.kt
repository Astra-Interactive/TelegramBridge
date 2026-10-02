@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.impl.role.internal

import kotlinx.coroutines.test.runTest
import net.luckperms.api.LuckPerms
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider
import ru.astrainteractive.messagebridge.link.impl.role.model.PermissionGroupError
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class LuckPermsGroupsTest {
    private val notLoaded = IllegalStateException("The LuckPerms API isn't loaded yet")
    private val groups = LuckPermsGroups(
        luckPermsProvider = object : LuckPermsProvider {
            override fun provide(): Result<LuckPerms> = Result.failure(notLoaded)
        }
    )
    private val uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000001")

    @Test
    fun GIVEN_luckperms_is_not_loaded_WHEN_group_is_added_THEN_fails_with_the_reason() = runTest {
        val t = groups.add(uuid, "linked").exceptionOrNull()

        assertIs<PermissionGroupError>(t)
        assertSame(notLoaded, t.cause)
    }

    @Test
    fun GIVEN_luckperms_is_not_loaded_WHEN_group_is_removed_THEN_fails_with_the_reason() = runTest {
        val t = groups.remove(uuid, "linked").exceptionOrNull()

        assertIs<PermissionGroupError>(t)
        assertSame(notLoaded, t.cause)
    }
}
