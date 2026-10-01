package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network

import okhttp3.Interceptor
import okhttp3.Response
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.impl.mapping.TelegramFailureMapper
import java.io.IOException
import java.net.HttpURLConnection
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class GetUpdatesStatusInterceptor(
    private val botName: String,
    private val failureMapper: TelegramFailureMapper,
    private val timeSource: TimeSource,
    private val onState: (TelegramConnectionState) -> Unit,
) : Interceptor {
    @Volatile
    private var isActive = true

    @Volatile
    private var lastConflict: TimeMark? = null

    private fun report(state: TelegramConnectionState) {
        if (isActive) onState(state)
    }

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
            lastConflict = timeSource.markNow()
            TelegramConnectionState.Failed(TelegramFailure.TokenInUse)
        }

        HttpURLConnection.HTTP_UNAUTHORIZED,
        HttpURLConnection.HTTP_NOT_FOUND -> TelegramConnectionState.Failed(TelegramFailure.InvalidToken)

        else -> null
    }

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
        } catch (exception: IOException) {
            if (!chain.call().isCanceled()) report(TelegramConnectionState.Failed(failureMapper.map(exception)))
            throw exception
        }
        stateOf(response.code)?.let(::report)
        return response
    }

    private companion object {
        val CONFLICT_MEMORY = 2.minutes
    }
}
