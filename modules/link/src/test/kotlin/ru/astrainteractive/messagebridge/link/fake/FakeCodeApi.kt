package ru.astrainteractive.messagebridge.link.fake

import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.model.CodeUser

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
