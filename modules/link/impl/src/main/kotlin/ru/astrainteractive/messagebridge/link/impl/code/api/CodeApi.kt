package ru.astrainteractive.messagebridge.link.impl.code.api

import ru.astrainteractive.messagebridge.link.impl.code.model.CodeUser

internal interface CodeApi {
    suspend fun generateCodeForPlayer(codeUser: CodeUser): Int
    suspend fun clearCode(code: Int)
    suspend fun findUserByCode(code: Int): CodeUser?
}
