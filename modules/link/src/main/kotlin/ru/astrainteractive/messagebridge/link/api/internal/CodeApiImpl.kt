package ru.astrainteractive.messagebridge.link.api.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.model.CodeUser
import kotlin.random.Random

class CodeApiImpl : CodeApi {
    private val cache = HashMap<CodeUser, Int>()

    private val mutex = Mutex()

    override suspend fun generateCodeForPlayer(codeUser: CodeUser): Int = mutex.withLock {
        cache[codeUser]?.let { return it }
        val codes = cache.values
        var code: Int
        do {
            code = Random.nextInt(0, 9999)
        } while (code in codes)
        cache[codeUser] = code
        code
    }

    override suspend fun findUserByCode(code: Int): CodeUser? {
        return cache.filter { it.value == code }.keys.firstOrNull()
    }

    override suspend fun clearCode(code: Int) {
        val key = findUserByCode(code)
        cache.remove(key)
    }
}
