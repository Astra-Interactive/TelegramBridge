package ru.astrainteractive.messagebridge.link.code.api

import ru.astrainteractive.messagebridge.link.code.model.CodeUser

internal interface LinkCodeRepository {
    suspend fun generateCodeForPlayer(codeUser: CodeUser): Int
    suspend fun clearCode(code: Int)
    suspend fun findUserByCode(code: Int): CodeUser?
}
