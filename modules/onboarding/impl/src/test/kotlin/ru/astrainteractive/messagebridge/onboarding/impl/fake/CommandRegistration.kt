package ru.astrainteractive.messagebridge.onboarding.impl.fake

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.CoroutineScope

internal data class CommandRegistration(
    val node: LiteralArgumentBuilder<*>,
    val scope: CoroutineScope
)
