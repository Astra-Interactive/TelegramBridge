@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.code.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import ru.astrainteractive.messagebridge.link.code.memory.InMemoryLinkCodeRepository
import ru.astrainteractive.messagebridge.link.code.model.CodeUser
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class LinkCodeRepositoryTest {
    private val linkCodeRepository = InMemoryLinkCodeRepository()
    private val steve = CodeUser(name = "Steve", uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"))
    private val alex = CodeUser(name = "Alex", uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000003"))

    @Test
    fun GIVEN_player_with_a_code_WHEN_asks_again_THEN_gets_the_same_code() = runTest {
        val first = linkCodeRepository.generateCodeForPlayer(steve)

        assertEquals(first, linkCodeRepository.generateCodeForPlayer(steve))
    }

    @Test
    fun GIVEN_two_players_WHEN_codes_are_generated_THEN_codes_differ() = runTest {
        assertNotEquals(linkCodeRepository.generateCodeForPlayer(steve), linkCodeRepository.generateCodeForPlayer(alex))
    }

    @Test
    fun GIVEN_generated_code_WHEN_looked_up_THEN_returns_its_player() = runTest {
        val code = linkCodeRepository.generateCodeForPlayer(steve)

        assertEquals(steve, linkCodeRepository.findUserByCode(code))
    }

    @Test
    fun GIVEN_cleared_code_WHEN_looked_up_THEN_nobody_owns_it() = runTest {
        val code = linkCodeRepository.generateCodeForPlayer(steve)

        linkCodeRepository.clearCode(code)

        assertNull(linkCodeRepository.findUserByCode(code))
    }

    @Test
    fun GIVEN_cleared_code_WHEN_player_asks_again_THEN_gets_a_code_that_works() = runTest {
        linkCodeRepository.clearCode(linkCodeRepository.generateCodeForPlayer(steve))

        val code = linkCodeRepository.generateCodeForPlayer(steve)

        assertEquals(steve, linkCodeRepository.findUserByCode(code))
    }

    @Test
    fun GIVEN_no_codes_WHEN_unknown_code_is_looked_up_THEN_nobody_owns_it() = runTest {
        assertNull(linkCodeRepository.findUserByCode(UNKNOWN_CODE))
    }

    @Test
    fun GIVEN_many_players_WHEN_they_get_and_redeem_codes_at_the_same_time_THEN_every_code_finds_its_player() =
        runTest {
            val players = List(PLAYER_COUNT) { index -> CodeUser(name = "Player$index", uuid = UUID.randomUUID()) }

            val found = withContext(Dispatchers.Default) {
                players.map { player ->
                    async {
                        val code = linkCodeRepository.generateCodeForPlayer(player)
                        val owner = linkCodeRepository.findUserByCode(code)
                        linkCodeRepository.clearCode(code)
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
