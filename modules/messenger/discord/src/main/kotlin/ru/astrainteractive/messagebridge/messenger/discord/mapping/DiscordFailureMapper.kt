package ru.astrainteractive.messagebridge.messenger.discord.mapping

import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.exceptions.InvalidTokenException
import net.dv8tion.jda.api.requests.CloseCode
import net.dv8tion.jda.api.requests.ErrorResponse
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.DiscordTranslation
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailureException
import ru.astrainteractive.messagebridge.messenger.discord.model.JdaShutdownException
import java.io.IOException

internal class DiscordFailureMapper {

    fun map(throwable: Throwable, config: PluginConfiguration.JdaConfig): DiscordFailure {
        val causes = generateSequence(throwable, Throwable::cause).take(MAX_CAUSES).toList()
        val ioException = causes.filterIsInstance<IOException>().firstOrNull()
        return when {
            throwable is DiscordFailureException -> throwable.failure
            causes.any { cause -> cause is InvalidTokenException } -> DiscordFailure.InvalidToken
            throwable is JdaShutdownException -> mapShutdown(throwable)
            throwable is InsufficientPermissionException -> DiscordFailure.MissingPermission(
                permission = throwable.permission.getName()
            )

            throwable is ErrorResponseException && throwable.errorResponse == ErrorResponse.UNKNOWN_CHANNEL -> {
                DiscordFailure.ChannelNotFound(config.channelId)
            }

            ioException != null -> DiscordFailure.Network(
                error = ioException.describe(),
                proxy = config.proxy?.let { proxy -> "${proxy.host}:${proxy.port}" }
            )

            else -> DiscordFailure.Unknown(throwable.describe())
        }
    }

    fun toText(failure: DiscordFailure, translation: DiscordTranslation): LocalizableComponent {
        val errors = translation.errors
        return when (failure) {
            DiscordFailure.InvalidToken -> errors.invalidToken
            DiscordFailure.MissingIntent -> errors.missingIntent
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

    private fun mapShutdown(exception: JdaShutdownException): DiscordFailure {
        val closeCode = exception.code?.let(CloseCode::from)
        return when (closeCode) {
            CloseCode.DISALLOWED_INTENTS -> DiscordFailure.MissingIntent
            CloseCode.AUTHENTICATION_FAILED -> DiscordFailure.InvalidToken
            null -> DiscordFailure.Unknown(exception.describe())
            else -> DiscordFailure.Unknown("${closeCode.code}: ${closeCode.meaning}")
        }
    }

    private fun Throwable.describe(): String {
        return listOfNotNull(this::class.java.simpleName, message?.takeIf(String::isNotBlank)).joinToString(": ")
    }

    private companion object {
        const val MAX_CAUSES = 10
    }
}
