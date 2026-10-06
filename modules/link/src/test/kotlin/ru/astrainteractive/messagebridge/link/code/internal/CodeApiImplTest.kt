@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.code.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import ru.astrainteractive.messagebridge.link.code.model.CodeUser
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class CodeApiImplTest {
    private val codeApi = CodeApiImpl()
    private val steve = CodeUser(name = "Steve", uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"))
    private val alex = CodeUser(name = "Alex", uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000003"))

    @Test
    fun GIVEN_player_with_a_code_WHEN_asks_again_THEN_gets_the_same_code() = runTest {
        val first = codeApi.generateCodeForPlayer(steve)

        assertEquals(first, codeApi.generateCodeForPlayer(steve))
    }

    @Test
    fun GIVEN_two_players_WHEN_codes_are_generated_THEN_codes_differ() = runTest {
        assertNotEquals(codeApi.generateCodeForPlayer(steve), codeApi.generateCodeForPlayer(alex))
    }

    @Test
    fun GIVEN_generated_code_WHEN_looked_up_THEN_returns_its_player() = runTest {
        val code = codeApi.generateCodeForPlayer(steve)

        assertEquals(steve, codeApi.findUserByCode(code))
    }

    @Test
    fun GIVEN_cleared_code_WHEN_looked_up_THEN_nobody_owns_it() = runTest {
        val code = codeApi.generateCodeForPlayer(steve)

        codeApi.clearCode(code)

        assertNull(codeApi.findUserByCode(code))
    }

    @Test
    fun GIVEN_cleared_code_WHEN_player_asks_again_THEN_gets_a_code_that_works() = runTest {
        codeApi.clearCode(codeApi.generateCodeForPlayer(steve))

        val code = codeApi.generateCodeForPlayer(steve)

        assertEquals(steve, codeApi.findUserByCode(code))
    }

    @Test
    fun GIVEN_no_codes_WHEN_unknown_code_is_looked_up_THEN_nobody_owns_it() = runTest {
        assertNull(codeApi.findUserByCode(UNKNOWN_CODE))
    }

    @Test
    fun GIVEN_many_players_WHEN_they_get_and_redeem_codes_at_the_same_time_THEN_every_code_finds_its_player() =
        runTest {
            val players = List(PLAYER_COUNT) { index -> CodeUser(name = "Player$index", uuid = UUID.randomUUID()) }

            val found = withContext(Dispatchers.Default) {
                players.map { player ->
                    async {
                        val code = codeApi.generateCodeForPlayer(player)
                        val owner = codeApi.findUserByCode(code)
                        codeApi.clearCode(code)
                        owner
                    }
                }.awaitAll()
            }

            assertEquals(players, found)
        }

    private companion object {
        const val PLAYER_COUNT = 2000
        const val UNKNOWN_CODE = 1234
    }
}
