package ru.astrainteractive.messagebridge.onboarding.discord.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.dv8tion.jda.api.Permission
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordConnection
import ru.astrainteractive.messagebridge.onboarding.api.api.DiscordOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.api.model.BindCode
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus

internal class JdaDiscordOnboarding(
    private val connection: StateFlow<DiscordConnection>,
    private val failureTextMapper: DiscordFailureTextMapper,
    private val bindCodes: BindCodes,
    override val deliveryError: StateFlow<LocalizableComponent?>,
    scope: CoroutineScope,
) : DiscordOnboarding {
    override val status: StateFlow<MessengerStatus> = connection
        .map(::toStatus)
        .stateIn(scope, SharingStarted.Eagerly, MessengerStatus.Connecting)

    private fun toStatus(connection: DiscordConnection): MessengerStatus = when (connection) {
        DiscordConnection.Disabled -> MessengerStatus.Disabled
        DiscordConnection.Connecting -> MessengerStatus.Connecting
        is DiscordConnection.Connected -> MessengerStatus.Connected(connection.jda.selfUser.name)
        is DiscordConnection.Failed -> MessengerStatus.Failed(
            reason = failureTextMapper.map(connection.failure)
        )
    }

    override fun issueBindCode(onBound: (LocalizableComponent) -> Unit): BindCode = bindCodes.issue(onBound)

    override fun inviteUrl(): String? {
        return connection.value
            .tryCast<DiscordConnection.Connected>()
            ?.jda
            ?.getInviteUrl(INVITE_PERMISSIONS)
    }

    private companion object {
        val INVITE_PERMISSIONS = listOf(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MANAGE_WEBHOOKS,
            Permission.MANAGE_CHANNEL,
            Permission.MANAGE_ROLES,
        )
    }
}
