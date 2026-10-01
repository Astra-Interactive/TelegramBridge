package ru.astrainteractive.messagebridge.messenger.telegram.impl.failure.mapping

import com.fasterxml.jackson.core.JsonProcessingException
import kotlinx.coroutines.flow.StateFlow
import org.telegram.telegrambots.longpolling.exceptions.TelegramApiErrorResponseException
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailure
import java.io.IOException
import java.net.HttpURLConnection
import kotlin.time.Duration.Companion.seconds

internal class TelegramFailureMapper(
    private val configFlow: StateFlow<PluginConfiguration>,
) {
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = configFlow.value.tgConfig

    private fun isInvalidTokenCode(code: Int?): Boolean {
        return code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_NOT_FOUND
    }

    private fun isServerErrorCode(code: Int?): Boolean {
        return code != null && code >= HttpURLConnection.HTTP_INTERNAL_ERROR
    }

    private fun mapDescription(description: String): TelegramFailure? = when {
        CHAT_NOT_SET in description -> TelegramFailure.ChatNotSet
        CHAT_NOT_FOUND in description -> TelegramFailure.ChatNotFound
        TOPIC_ERRORS.any(description::contains) -> TelegramFailure.TopicNotFound
        RIGHTS_ERRORS.any(description::contains) -> TelegramFailure.NoRights
        NOT_IN_CHAT_ERRORS.any(description::contains) -> TelegramFailure.BotNotInChat
        else -> null
    }

    private fun mapRequest(exception: TelegramApiRequestException): TelegramFailure? {
        val apiResponse: String? = exception.apiResponse
        val code = exception.errorCode ?: NO_CODE
        if (apiResponse == null && code == NO_CODE) {
            val isNotBotApi = exception.cause is JsonProcessingException && tgConfig.apiUrl.isNotBlank()
            return TelegramFailure.InvalidApiUrl.takeIf { isNotBotApi }
        }
        exception.parameters?.migrateToChatId?.let { chatId -> return TelegramFailure.ChatMigrated(chatId) }
        return when {
            isInvalidTokenCode(code) -> TelegramFailure.InvalidToken
            code == HttpURLConnection.HTTP_CONFLICT -> TelegramFailure.TokenInUse
            code == HTTP_TOO_MANY_REQUESTS -> TelegramFailure.RateLimited(exception.parameters?.retryAfter?.seconds)
            isServerErrorCode(code) -> TelegramFailure.ServerError(code)
            else -> mapDescription(apiResponse.orEmpty().lowercase())
                ?: TelegramFailure.BotNotInChat.takeIf { code == HttpURLConnection.HTTP_FORBIDDEN }
                ?: TelegramFailure.Unknown(apiResponse ?: "$code")
        }
    }

    private fun mapResponse(exception: TelegramApiErrorResponseException): TelegramFailure? {
        val code = exception.toString().substringBefore(':').toIntOrNull()
        return when {
            isInvalidTokenCode(code) -> TelegramFailure.InvalidToken
            code == HttpURLConnection.HTTP_CONFLICT -> TelegramFailure.TokenInUse
            code == HTTP_TOO_MANY_REQUESTS -> TelegramFailure.RateLimited(retryAfter = null)
            code != null && isServerErrorCode(code) -> TelegramFailure.ServerError(code)
            exception.message in INVALID_TOKEN_REASONS -> TelegramFailure.InvalidToken
            exception.message == CONFLICT_REASON -> TelegramFailure.TokenInUse
            code != null && code > NO_CODE -> TelegramFailure.Unknown("$code ${exception.message.orEmpty()}".trim())
            else -> null
        }
    }

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
            root.takeIf { root !== first }?.let { cause -> "${cause::class.java.simpleName}: ${cause.message}" }
        ).joinToString(separator = ": ") { message -> message.lineSequence().first() }
            .ifBlank { first::class.java.simpleName }
    }

    fun map(throwable: Throwable): TelegramFailure {
        val causes = generateSequence(throwable) { current -> current.cause?.takeIf { cause -> cause !== current } }
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

    private companion object {
        const val MAX_CAUSES = 16
        const val NO_CODE = 0
        const val HTTP_TOO_MANY_REQUESTS = 429
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
