package ru.astrainteractive.messagebridge.onboarding.fake

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.CoroutineScope

/** @property scope the command stays registered until it is cancelled */
internal data class CommandRegistration(
    val node: LiteralArgumentBuilder<*>,
    val scope: CoroutineScope
)
