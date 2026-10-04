package ru.astrainteractive.messagebridge.messenger.discord.messaging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRoleChange
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await

internal class DiscordRoleUpdater(
    private val discordChannel: Flow<DiscordChannel>,
    roleChanges: Flow<DiscordRoleChange>
) : CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-DiscordRoleUpdater").withoutParentHandlers() {

    private fun DiscordRoleChange.restAction(guild: Guild, role: Role): AuditableRestAction<Void> {
        val user = UserSnowflake.fromId(discordUserId)
        return when (this) {
            is DiscordRoleChange.Grant -> guild.addRoleToMember(user, role)
        }
    }

    suspend fun update(change: DiscordRoleChange) {
        val ready = discordChannel
            .first { state -> state is DiscordChannel.Ready || state == DiscordChannel.Disabled }
            .tryCast<DiscordChannel.Ready>()
            ?: run {
                info { "#update Discord is not configured, skipped $change" }
                return
            }
        val guild = ready.textChannel.guild
        val role = guild.getRoleById(change.roleId) ?: run {
            error { "#update could not find role ${change.roleId}" }
            return
        }
        runCatching { change.restAction(guild, role).await() }
            .propagateCancellationException()
            .onFailure { t -> warn { "#update could not apply $change: ${t.message}" } }
    }

    init {
        roleChanges
            .onEach(::update)
            .launchIn(this)
    }
}
