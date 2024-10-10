package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AtomicList<T> {
    private val list = mutableListOf<T>()
    private val mutex = Mutex()

    fun toList() = list.toList()

    suspend fun removeWhere(block: suspend (T) -> Boolean) = mutex.withLock {
        val i = list.indexOfFirst { item -> block.invoke(item) }
        if (i == -1) {
            null
        } else {
            list.removeAt(i)
        }
    }

    suspend fun add(item: T) = mutex.withLock {
        list.add(item)
    }

    suspend fun remove(item: T) = mutex.withLock {
        list.remove(item)
    }

    suspend fun removeAt(index: Int) = mutex.withLock {
        list.removeAt(index)
    }
}
