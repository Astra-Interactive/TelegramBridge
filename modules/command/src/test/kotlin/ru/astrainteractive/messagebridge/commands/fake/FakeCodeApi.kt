package ru.astrainteractive.messagebridge.commands.fake

import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.model.CodeUser

/**
 * Hands out [code] to everyone who asks, so a test knows which code a player reads; a [failure] is thrown instead,
 * like a code service that breaks.
 */
internal class FakeCodeApi(
    private val code: Int,
    private val failure: Throwable? = null
) : CodeApi {
    val codeUsers = mutableListOf<CodeUser>()

    override suspend fun generateCodeForPlayer(codeUser: CodeUser): Int {
        if (failure != null) {
            throw failure
        }
        codeUsers.add(codeUser)
        return code
    }

    override suspend fun clearCode(code: Int) {
        error("Minecraft commands never redeem a code")
    }

    override suspend fun findUserByCode(code: Int): CodeUser? {
        error("Minecraft commands never redeem a code")
    }
}
