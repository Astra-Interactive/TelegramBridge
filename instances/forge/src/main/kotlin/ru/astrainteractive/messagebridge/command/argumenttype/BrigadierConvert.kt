package ru.astrainteractive.messagebridge.command.argumenttype

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType

fun <T : Any> ru.astrainteractive.astralibs.command.api.argumenttype.ArgumentType<T>.toBrigadier(): ArgumentType<T> {
    val astraArgumentType = this
    return object : ArgumentType<T> {
        override fun parse(reader: StringReader): T {
            try {
                val result = astraArgumentType.transform(reader.readString())
                return result
            } catch (t: Throwable) {
                val message = LiteralMessage(t.message ?: t.localizedMessage)
                val type = SimpleCommandExceptionType(message)
                val exception = CommandSyntaxException(type, message)
                throw exception
            }
        }
    }
}
