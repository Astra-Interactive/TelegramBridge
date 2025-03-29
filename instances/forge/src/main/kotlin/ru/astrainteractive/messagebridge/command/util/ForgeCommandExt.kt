package ru.astrainteractive.messagebridge.command.util

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraftforge.event.RegisterCommandsEvent

fun <T> ArgumentBuilder<CommandSourceStack, *>.argument(
    alias: String,
    type: ArgumentType<T>,
    builder: (RequiredArgumentBuilder<CommandSourceStack, T>.() -> Unit)? = null,
    execute: (RequiredArgumentBuilder<CommandSourceStack, T>.(CommandContext<CommandSourceStack>) -> Unit)? = null
) {
    val argument: RequiredArgumentBuilder<CommandSourceStack, T> = Commands.argument(alias, type)
    builder?.invoke(argument)
    execute?.let {
        argument.executes { commandContext ->
            execute.invoke(argument, commandContext)
            Command.SINGLE_SUCCESS
        }
    }
    then(argument)
}

/**
 * Example:
 * fun RegisterCommandsEvent.giveItemCommand() {
 *     command("giveitem2") {
 *         argument(
 *             alias = "player",
 *             type = StringArgumentType.string(),
 *             builder = {
 *                 argument(
 *                     alias = "truefalse",
 *                     type = PrimitiveArgumentType.Boolean.toBrigadier(),
 *                     builder = {
 *                         argument(
 *                             alias = "amount",
 *                             type = IntegerArgumentType.integer(1, 4),
 *                             execute = {
 *                                 println("Player -> Item -> amount")
 *                             }
 *                         )
 *                     },
 *                     execute = {
 *                         println("Player -> Item")
 *                     }
 *                 )
 *             },
 *         )
 *     }
 * }
 */
fun RegisterCommandsEvent.command(alias: String, block: LiteralArgumentBuilder<CommandSourceStack>.() -> Unit) {
    val literal = Commands.literal(alias)
    literal.block()
    dispatcher.register(literal)
}
