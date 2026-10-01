package ru.astrainteractive.messagebridge.messenger.telegram.util

import okhttp3.Interceptor
import okhttp3.Response
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramFailureMapper
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramFailure
import java.io.IOException
import java.net.HttpURLConnection
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * Turns the answers to the getUpdates polling into [TelegramConnectionState], because the library retries the
 * polling on its own and keeps its errors to itself.
 */
internal class GetUpdatesStatusInterceptor(
    private val botName: String,
    private val failureMapper: TelegramFailureMapper,
    private val onState: (TelegramConnectionState) -> Unit,
) : Interceptor {
    @Volatile
    private var isActive = true

    @Volatile
    private var lastConflict: TimeSource.Monotonic.ValueTimeMark? = null

    /** Stops reporting, so a closing session does not overwrite the state of the next one. */
    fun deactivate() {
        isActive = false
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.pathSegments.last().equals(GetUpdates.PATH, ignoreCase = true)) {
            return chain.proceed(request)
        }
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (!chain.call().isCanceled()) report(TelegramConnectionState.Failed(failureMapper.map(e)))
            throw e
        }
        stateOf(response.code)?.let(::report)
        return response
    }

    /**
     * Two programs with one token take turns in getting 409, so a success soon after a conflict does not mean the
     * other one has stopped.
     */
    private fun stateOf(code: Int): TelegramConnectionState? = when (code) {
        HttpURLConnection.HTTP_OK -> {
            val isAfterConflict = lastConflict?.let { mark -> mark.elapsedNow() < CONFLICT_MEMORY } == true
            if (isAfterConflict) {
                TelegramConnectionState.Failed(TelegramFailure.TokenInUse)
            } else {
                TelegramConnectionState.Connected(botName)
            }
        }

        HttpURLConnection.HTTP_CONFLICT -> {
            lastConflict = TimeSource.Monotonic.markNow()
            TelegramConnectionState.Failed(TelegramFailure.TokenInUse)
        }

        HttpURLConnection.HTTP_UNAUTHORIZED,
        HttpURLConnection.HTTP_NOT_FOUND -> TelegramConnectionState.Failed(TelegramFailure.InvalidToken)

        else -> null
    }

    private fun report(state: TelegramConnectionState) {
        if (isActive) onState(state)
    }

    private companion object {
        val CONFLICT_MEMORY = 2.minutes
    }
}
