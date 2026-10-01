package ru.astrainteractive.messagebridge.link.code

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/** Keeps one code per player until it is used, so asking again shows the same code. */
internal class CodeApiImpl(
    private val random: Random
) : CodeApi {
    private val codeByUser = HashMap<CodeUser, Int>()
    private val mutex = Mutex()

    private fun findUser(code: Int): CodeUser? {
        return codeByUser.entries.firstOrNull { entry -> entry.value == code }?.key
    }

    private fun newCode(): Int {
        val usedCodes = codeByUser.values.toSet()
        var code: Int
        do {
            code = random.nextInt(0, MAX_CODE)
        } while (code in usedCodes)
        return code
    }

    override suspend fun generateCodeForPlayer(codeUser: CodeUser): Int {
        return mutex.withLock {
            codeByUser.getOrPut(codeUser, ::newCode)
        }
    }

    override suspend fun findUserByCode(code: Int): CodeUser? {
        return mutex.withLock { findUser(code) }
    }

    override suspend fun clearCode(code: Int) {
        mutex.withLock {
            val user = findUser(code) ?: return@withLock
            codeByUser.remove(user)
        }
    }

    private companion object {
        const val MAX_CODE = 9999
    }
}
