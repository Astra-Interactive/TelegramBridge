package ru.astrainteractive.messagebridge.link.api

import ru.astrainteractive.messagebridge.link.api.model.CodeUser

interface CodeApi {
    suspend fun generateCodeForPlayer(codeUser: CodeUser): Int
    suspend fun clearCode(code: Int)
    suspend fun getUser(code: Int): CodeUser?
}
