package ru.astrainteractive.messagebridge.messaging.setup

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** One-time codes that let an admin bind a chat by sending the code into it. */
class BindCodes(
    private val clock: Clock = Clock.System,
    private val lifetime: Duration = LIFETIME
) {
    private class Request(val expiresAt: Instant, val onBound: (LocalizableComponent) -> Unit)

    private val random = SecureRandom()
    private val requests = ConcurrentHashMap<String, Request>()

    fun issue(onBound: (LocalizableComponent) -> Unit): String {
        val now = clock.now()
        requests.entries.removeIf { entry -> entry.value.expiresAt < now }
        val code = (1..CODE_LENGTH).joinToString(separator = "") { "${random.nextInt(DIGITS)}" }
        requests[code] = Request(expiresAt = now + lifetime, onBound = onBound)
        return code
    }

    fun isValid(code: String): Boolean {
        val request = requests[code] ?: return false
        return request.expiresAt >= clock.now()
    }

    /** @return the callback of the request, or `null` when the code is unknown or expired */
    fun consume(code: String): ((LocalizableComponent) -> Unit)? {
        val request = requests.remove(code) ?: return null
        return request.onBound.takeIf { request.expiresAt >= clock.now() }
    }

    companion object {
        val LIFETIME = 10.minutes
        private const val CODE_LENGTH = 8
        private const val DIGITS = 10
    }
}
