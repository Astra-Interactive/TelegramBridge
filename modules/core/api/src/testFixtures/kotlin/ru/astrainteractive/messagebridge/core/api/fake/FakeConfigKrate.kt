package ru.astrainteractive.messagebridge.core.api.fake

import ru.astrainteractive.klibs.kstorage.api.MutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import java.util.concurrent.CopyOnWriteArrayList

class FakeConfigKrate(
    initial: Result<PluginConfiguration>
) : MutableKrate<Result<PluginConfiguration>> {
    private var stored: Result<PluginConfiguration> = initial

    val saves: MutableList<PluginConfiguration> = CopyOnWriteArrayList()

    val configuration: PluginConfiguration?
        get() = stored.getOrNull()

    override fun getValue(): Result<PluginConfiguration> = stored

    override fun save(value: Result<PluginConfiguration>) {
        val configuration = value.getOrNull() ?: return
        stored = value
        saves += configuration
    }

    override fun save(block: (Result<PluginConfiguration>) -> Result<PluginConfiguration>) {
        save(block.invoke(stored))
    }

    override fun saveAndGet(
        block: (Result<PluginConfiguration>) -> Result<PluginConfiguration>
    ): Result<PluginConfiguration> {
        val value = block.invoke(stored)
        save(value)
        return value
    }

    override fun reset() {
        save(Result.success(PluginConfiguration()))
    }

    override fun resetAndGet(): Result<PluginConfiguration> {
        reset()
        return stored
    }
}
