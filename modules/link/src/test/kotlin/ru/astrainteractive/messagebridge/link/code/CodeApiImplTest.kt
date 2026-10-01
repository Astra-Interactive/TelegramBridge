@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.code

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CodeApiImplTest {
    private val codeApi = CodeApiImpl(Random(SEED))
    private val steve = CodeUser(name = "Steve", uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"))

    @Test
    fun GIVEN_player_asks_again_WHEN_code_is_made_THEN_the_same_code_is_shown() = runTest {
        val first = codeApi.generateCodeForPlayer(steve)

        assertEquals(first, codeApi.generateCodeForPlayer(steve))
        assertEquals(steve, codeApi.findUserByCode(first))
    }

    @Test
    fun GIVEN_used_code_WHEN_found_THEN_nobody_is_found() = runTest {
        val code = codeApi.generateCodeForPlayer(steve)

        codeApi.clearCode(code)

        assertNull(codeApi.findUserByCode(code))
    }

    @Test
    fun GIVEN_unknown_code_WHEN_cleared_THEN_the_codes_of_others_stay() = runTest {
        val code = codeApi.generateCodeForPlayer(steve)

        codeApi.clearCode(code + 1)

        assertEquals(steve, codeApi.findUserByCode(code))
    }

    @Test
    fun GIVEN_many_players_ask_at_once_WHEN_codes_are_made_THEN_every_player_gets_an_own_code() = runTest {
        val players = (1..PLAYERS).map { index -> CodeUser(name = "Player$index", uuid = UUID.randomUUID()) }

        val codes = withContext(Dispatchers.Default) {
            players.map { player -> async { codeApi.generateCodeForPlayer(player) } }.awaitAll()
        }

        assertEquals(PLAYERS, codes.toSet().size)
        players.forEachIndexed { index, player -> assertEquals(player, codeApi.findUserByCode(codes[index])) }
    }

    private companion object {
        const val SEED = 1
        const val PLAYERS = 500
    }
}
