package ru.astrainteractive.messagebridge.messenger.telegram.mapping

import com.fasterxml.jackson.core.JsonProcessingException
import kotlinx.coroutines.flow.StateFlow
import org.telegram.telegrambots.longpolling.exceptions.TelegramApiErrorResponseException
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramFailure
import java.io.IOException

internal class TelegramFailureMapper(
    private val configFlow: StateFlow<PluginConfiguration>,
) {
    private val config: PluginConfiguration
        get() = configFlow.value
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = config.tgConfig

    fun map(throwable: Throwable): TelegramFailure {
        val causes = generateSequence(throwable) { cause -> cause.cause?.takeIf { it !== cause } }
            .take(MAX_CAUSES)
            .toList()
        causes.firstNotNullOfOrNull { cause -> (cause as? TelegramApiRequestException)?.let(::mapRequest) }
            ?.let { failure -> return failure }
        causes.firstNotNullOfOrNull { cause -> (cause as? TelegramApiErrorResponseException)?.let(::mapResponse) }
            ?.let { failure -> return failure }
        if (causes.any(::isProxyAuthFailure)) return TelegramFailure.ProxyAuth
        if (causes.any { cause -> cause is IOException && cause !is JsonProcessingException }) {
            return TelegramFailure.Network(
                proxy = tgConfig.proxy,
                apiUrl = tgConfig.apiUrl.trim().ifBlank { null }
            )
        }
        return TelegramFailure.Unknown(describe(causes))
    }

    /** @return `null` when Telegram did not answer, so the cause of the exception tells what happened */
    @Suppress("MagicNumber")
    private fun mapRequest(exception: TelegramApiRequestException): TelegramFailure? {
        val apiResponse: String? = exception.apiResponse
        val code = exception.errorCode ?: 0
        if (apiResponse == null && code == 0) {
            val isNotBotApi = exception.cause is JsonProcessingException && tgConfig.apiUrl.isNotBlank()
            return TelegramFailure.InvalidApiUrl.takeIf { isNotBotApi }
        }
        exception.parameters?.migrateToChatId?.let { chatId -> return TelegramFailure.ChatMigrated(chatId) }
        return when {
            code == 401 || code == 404 -> TelegramFailure.InvalidToken
            code == 409 -> TelegramFailure.TokenInUse
            code == 429 -> TelegramFailure.RateLimited(exception.parameters?.retryAfter)
            code >= 500 -> TelegramFailure.ServerError(code)
            else -> mapDescription(apiResponse.orEmpty().lowercase())
                ?: TelegramFailure.BotNotInChat.takeIf { code == 403 }
                ?: TelegramFailure.Unknown(apiResponse ?: "$code")
        }
    }

    private fun mapDescription(description: String): TelegramFailure? = when {
        CHAT_NOT_SET in description -> TelegramFailure.ChatNotSet
        CHAT_NOT_FOUND in description -> TelegramFailure.ChatNotFound
        TOPIC_ERRORS.any(description::contains) -> TelegramFailure.TopicNotFound
        RIGHTS_ERRORS.any(description::contains) -> TelegramFailure.NoRights
        NOT_IN_CHAT_ERRORS.any(description::contains) -> TelegramFailure.BotNotInChat
        else -> null
    }

    /**
     * The HTTP code of a failed getUpdates is private and shows only in [TelegramApiErrorResponseException.toString],
     * as `401: …`; the message is the HTTP reason, which HTTP/2 leaves empty.
     *
     * @return `null` when the request did not reach Telegram
     */
    @Suppress("MagicNumber")
    private fun mapResponse(exception: TelegramApiErrorResponseException): TelegramFailure? {
        val code = exception.toString().substringBefore(':').toIntOrNull()
        return when {
            code == 401 || code == 404 -> TelegramFailure.InvalidToken
            code == 409 -> TelegramFailure.TokenInUse
            code == 429 -> TelegramFailure.RateLimited(retryAfter = null)
            code != null && code >= 500 -> TelegramFailure.ServerError(code)
            exception.message in INVALID_TOKEN_REASONS -> TelegramFailure.InvalidToken
            exception.message == CONFLICT_REASON -> TelegramFailure.TokenInUse
            code != null && code > 0 -> TelegramFailure.Unknown("$code ${exception.message.orEmpty()}".trim())
            else -> null
        }
    }

    /** OkHttp gives up on a proxy that keeps answering 407 with one of these messages. */
    private fun isProxyAuthFailure(throwable: Throwable): Boolean {
        if (throwable !is IOException) return false
        val message = throwable.message.orEmpty()
        return PROXY_AUTH_ERRORS.any(message::startsWith)
    }

    private fun describe(causes: List<Throwable>): String {
        val first = causes.first()
        val root = causes.last()
        return listOfNotNull(
            first.message,
            root.takeIf { root !== first }?.let { "${it::class.java.simpleName}: ${it.message}" }
        ).joinToString(separator = ": ") { message -> message.lineSequence().first() }
            .ifBlank { first::class.java.simpleName }
    }

    private companion object {
        const val MAX_CAUSES = 16
        const val CHAT_NOT_SET = "chat_id is empty"
        const val CHAT_NOT_FOUND = "chat not found"
        const val CONFLICT_REASON = "Conflict"
        val INVALID_TOKEN_REASONS = setOf("Unauthorized", "Not Found")
        val TOPIC_ERRORS = listOf(
            "message thread not found",
            "topic_closed",
            "topic_deleted",
            "topic_id_invalid",
            "message to be replied not found",
            "replied message not found"
        )
        val RIGHTS_ERRORS = listOf(
            "not enough rights",
            "have no rights",
            "chat_write_forbidden",
            "chat_admin_required",
            "need administrator rights",
            "message can't be deleted"
        )
        val NOT_IN_CHAT_ERRORS = listOf(
            "bot was kicked",
            "bot is not a member",
            "bot was blocked"
        )
        val PROXY_AUTH_ERRORS = listOf(
            "Failed to authenticate with proxy",
            "Too many tunnel connections attempted"
        )
    }
}
