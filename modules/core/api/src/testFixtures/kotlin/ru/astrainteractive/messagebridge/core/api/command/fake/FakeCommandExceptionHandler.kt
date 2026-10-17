package ru.astrainteractive.messagebridge.core.api.command.fake

import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineExceptionHandler
import ru.astrainteractive.messagebridge.core.api.command.api.CommandExceptionHandler
import java.util.Collections

class FakeCommandExceptionHandler : CommandExceptionHandler {
    val failures: MutableList<Throwable> = Collections.synchronizedList(mutableListOf())

    override fun handle(ctx: CommandContext<Any>, throwable: Throwable) {
        failures += throwable
    }

    override fun coroutineExceptionHandler(ctx: CommandContext<Any>): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable -> failures += throwable }
    }
}
