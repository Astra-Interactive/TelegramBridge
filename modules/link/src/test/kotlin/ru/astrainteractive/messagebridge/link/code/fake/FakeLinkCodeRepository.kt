package ru.astrainteractive.messagebridge.link.code.fake

import ru.astrainteractive.messagebridge.link.code.api.LinkCodeRepository
import ru.astrainteractive.messagebridge.link.code.model.CodeUser

internal class FakeLinkCodeRepository(
    private val code: Int,
    private val failure: Throwable? = null
) : LinkCodeRepository {
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
