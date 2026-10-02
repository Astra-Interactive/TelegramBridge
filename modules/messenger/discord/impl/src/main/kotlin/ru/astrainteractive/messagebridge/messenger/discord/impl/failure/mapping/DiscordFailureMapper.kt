package ru.astrainteractive.messagebridge.messenger.discord.impl.failure.mapping

import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.exceptions.InvalidTokenException
import net.dv8tion.jda.api.requests.CloseCode
import net.dv8tion.jda.api.requests.ErrorResponse
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.api.config.DiscordTranslation
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.impl.failure.model.DiscordFailureError
import java.io.IOException

internal class DiscordFailureMapper {

    private fun Throwable.describe(): String {
        return listOfNotNull(this::class.java.simpleName, message?.takeIf(String::isNotBlank)).joinToString(": ")
    }

    fun map(t: Throwable, config: PluginConfiguration.JdaConfig): DiscordFailure {
        val causes = generateSequence(t, Throwable::cause).take(MAX_CAUSES).toList()
        val ioException = causes.filterIsInstance<IOException>().firstOrNull()
        return when {
            t is DiscordFailureError -> t.failure
            causes.any { cause -> cause is InvalidTokenException } -> DiscordFailure.InvalidToken
            t is InsufficientPermissionException -> DiscordFailure.MissingPermission(
                permission = t.permission.getName()
            )

            t is ErrorResponseException && t.errorResponse == ErrorResponse.UNKNOWN_CHANNEL -> {
                DiscordFailure.ChannelNotFound(config.channelId)
            }

            ioException != null -> DiscordFailure.Network(
                error = ioException.describe(),
                proxy = config.proxy?.let { proxy -> "${proxy.host}:${proxy.port}" }
            )

            else -> DiscordFailure.Unknown(t.describe())
        }
    }

    fun mapShutdown(closeCode: Int?, privilegedIntents: List<String>): DiscordFailure {
        if (closeCode == null) return DiscordFailure.Unknown("JDA stopped without a close code")
        return when (val code = CloseCode.from(closeCode)) {
            CloseCode.DISALLOWED_INTENTS -> DiscordFailure.MissingIntent(privilegedIntents)
            CloseCode.AUTHENTICATION_FAILED -> DiscordFailure.InvalidToken
            null -> DiscordFailure.Unknown("JDA stopped with close code $closeCode")
            else -> DiscordFailure.Unknown("${code.code}: ${code.meaning}")
        }
    }

    fun toText(failure: DiscordFailure, translation: DiscordTranslation): LocalizableComponent {
        val errors = translation.errors
        return when (failure) {
            DiscordFailure.InvalidToken -> errors.invalidToken
            is DiscordFailure.MissingIntent -> errors.missingIntents(failure.intents.joinToString(separator = ", "))
            DiscordFailure.ChannelNotSet -> errors.channelNotSet
            is DiscordFailure.ChannelNotFound -> errors.channelNotFound(failure.channelId)
            is DiscordFailure.MissingPermission -> errors.missingPermission(failure.permission)
            DiscordFailure.SocksNotSupported -> errors.socksNotSupported
            is DiscordFailure.Network -> when (val proxy = failure.proxy) {
                null -> errors.network(failure.error)
                else -> errors.networkViaProxy(proxy = proxy, error = failure.error)
            }

            is DiscordFailure.Unknown -> errors.unknown(failure.error)
        }
    }

    private companion object {
        const val MAX_CAUSES = 10
    }
}
