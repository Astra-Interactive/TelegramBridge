package ru.astrainteractive.messagebridge.messenger.discord.onboarding.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.discord.connection.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.connection.model.awaitJda
import ru.astrainteractive.messagebridge.messenger.discord.failure.internal.DiscordDeliveryError
import ru.astrainteractive.messagebridge.messenger.discord.failure.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.onboarding.api.api.DiscordOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.api.model.BindCode
import ru.astrainteractive.messagebridge.onboarding.api.model.Check
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import kotlin.time.Duration.Companion.seconds

internal class JdaDiscordOnboarding(
    private val connection: StateFlow<DiscordConnection>,
    private val failureMapper: DiscordFailureMapper,
    private val bindCodes: BindCodes,
    private val diagnostics: DiscordDiagnostics,
    delivery: DiscordDeliveryError,
    translationKrate: CachedKrate<PluginTranslation>,
    scope: CoroutineScope,
) : DiscordOnboarding {
    private val translation by translationKrate

    override val status: StateFlow<MessengerStatus> = connection
        .map(::toStatus)
        .stateIn(scope, SharingStarted.Eagerly, MessengerStatus.Connecting)

    override val deliveryError: StateFlow<LocalizableComponent?> = delivery.text

    private fun toStatus(connection: DiscordConnection): MessengerStatus = when (connection) {
        DiscordConnection.Disabled -> MessengerStatus.Disabled
        DiscordConnection.Connecting -> MessengerStatus.Connecting
        is DiscordConnection.Connected -> MessengerStatus.Connected(connection.jda.selfUser.name)
        is DiscordConnection.Failed -> MessengerStatus.Failed(
            reason = failureMapper.toText(connection.failure, translation.discord)
        )
    }

    override fun issueBindCode(onBound: (LocalizableComponent) -> Unit): BindCode = bindCodes.issue(onBound)

    override suspend fun check(): List<Check> = diagnostics.diagnose()

    override suspend fun inviteUrl(): String? {
        return connection.awaitJda(INVITE_CONNECTION_WAIT)?.getInviteUrl(DiscordPermissions.ALL)
    }

    private companion object {
        val INVITE_CONNECTION_WAIT = 10.seconds
    }
}
