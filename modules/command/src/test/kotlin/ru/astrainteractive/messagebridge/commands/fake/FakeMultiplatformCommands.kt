package ru.astrainteractive.messagebridge.commands.fake

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommands
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KPlayerKCommandSender
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer

internal class FakeMultiplatformCommands(
    private val player: OnlineKPlayer
) : MultiplatformCommands {
    override fun literal(literal: String): LiteralArgumentBuilder<Any> {
        return LiteralArgumentBuilder.literal(literal)
    }

    override fun <T : Any> argument(
        name: String,
        argumentType: ArgumentType<T>
    ): RequiredArgumentBuilder<Any, T> {
        return RequiredArgumentBuilder.argument(name, argumentType)
    }

    override fun getSender(context: CommandContext<*>): KCommandSender = KPlayerKCommandSender(player)
}
