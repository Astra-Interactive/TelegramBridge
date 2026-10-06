package ru.astrainteractive.messagebridge.link.code.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.astrainteractive.messagebridge.link.code.api.CodeApi
import ru.astrainteractive.messagebridge.link.code.model.CodeUser
import kotlin.random.Random

internal class CodeApiImpl : CodeApi {
    private val cache = HashMap<CodeUser, Int>()

    private val mutex = Mutex()

    @Suppress("MagicNumber")
    override suspend fun generateCodeForPlayer(codeUser: CodeUser): Int = mutex.withLock {
        cache[codeUser]?.let { cachedCode -> return cachedCode }
        val codes = cache.values
        var code: Int
        do {
            code = Random.nextInt(0, 9999)
        } while (code in codes)
        cache[codeUser] = code
        code
    }

    override suspend fun findUserByCode(code: Int): CodeUser? = mutex.withLock {
        cache.entries
            .firstOrNull { entry -> entry.value == code }
            ?.key
    }

    override suspend fun clearCode(code: Int) {
        mutex.withLock {
            cache.entries.removeAll { entry -> entry.value == code }
        }
    }
}
