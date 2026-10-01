package ru.astrainteractive.messagebridge.link.code

interface CodeApi {
    suspend fun generateCodeForPlayer(codeUser: CodeUser): Int
    suspend fun clearCode(code: Int)
    suspend fun findUserByCode(code: Int): CodeUser?
}
