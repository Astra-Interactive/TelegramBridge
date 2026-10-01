package ru.astrainteractive.messagebridge.onboarding.fake

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.CoroutineScope
import ru.astrainteractive.astralibs.command.api.registrar.CommandRegistrarContext

internal class FakeCommandRegistrarContext : CommandRegistrarContext {
    val registrations = mutableListOf<CommandRegistration>()

    override fun registerWhenReady(node: LiteralArgumentBuilder<*>, scope: CoroutineScope) {
        registrations.add(CommandRegistration(node = node, scope = scope))
    }
}
