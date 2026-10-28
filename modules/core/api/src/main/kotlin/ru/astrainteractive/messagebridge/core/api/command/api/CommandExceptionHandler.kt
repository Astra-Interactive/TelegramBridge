@file:Suppress("FunctionVerb")

package ru.astrainteractive.messagebridge.core.api.command.api

import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineExceptionHandler

interface CommandExceptionHandler {
    fun handle(ctx: CommandContext<Any>, throwable: Throwable)

    fun coroutineExceptionHandler(ctx: CommandContext<Any>): CoroutineExceptionHandler
}
