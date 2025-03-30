package ru.astrainteractive.messagebridge.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraftforge.event.RegisterCommandsEvent
import ru.astrainteractive.astralibs.command.api.argumenttype.PrimitiveArgumentType
import ru.astrainteractive.messagebridge.forge.core.command.argumenttype.toBrigadier
import ru.astrainteractive.messagebridge.forge.core.command.util.argument
import ru.astrainteractive.messagebridge.forge.core.command.util.command

fun RegisterCommandsEvent.giveItemCommand() {
    command("giveitem2") {
        argument(
            alias = "player",
            type = StringArgumentType.string(),
            builder = {
                argument(
                    alias = "truefalse",
                    type = PrimitiveArgumentType.Boolean.toBrigadier(),
                    builder = {
                        argument(
                            alias = "amount",
                            type = IntegerArgumentType.integer(1, 4),
                            execute = {
                                println("Player -> Item -> amount")
                            }
                        )
                    },
                    execute = {
                        println("Player -> Item")
                    }
                )
            },
        )
    }
}
