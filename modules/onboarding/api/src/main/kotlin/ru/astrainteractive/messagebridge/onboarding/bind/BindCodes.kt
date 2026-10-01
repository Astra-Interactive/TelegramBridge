package ru.astrainteractive.messagebridge.onboarding.bind

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * One-time codes that let an admin bind a chat by sending the code into it.
 *
 * @param random source of the digits; a [java.security.SecureRandom] keeps the codes from being guessed
 */
class BindCodes(
    private val clock: Clock,
    private val lifetime: Duration,
    private val random: Random
) {
    private val requests = ConcurrentHashMap<String, BindRequest>()

    fun issue(onBound: (LocalizableComponent) -> Unit): BindCode {
        val now = clock.now()
        requests.entries.removeIf { entry -> entry.value.expiresAt < now }
        val value = (1..CODE_LENGTH).joinToString(separator = "") { _ -> "${random.nextInt(DIGITS)}" }
        requests[value] = BindRequest(expiresAt = now + lifetime, onBound = onBound)
        return BindCode(value = value, lifetime = lifetime)
    }

    fun isValid(code: String): Boolean {
        val request = requests[code] ?: return false
        return request.expiresAt >= clock.now()
    }

    /** @return the callback of the request, or `null` when the code is unknown or expired */
    fun consume(code: String): ((LocalizableComponent) -> Unit)? {
        val request = requests.remove(code) ?: return null
        return request.onBound.takeIf { _ -> request.expiresAt >= clock.now() }
    }

    private companion object {
        const val CODE_LENGTH = 8
        const val DIGITS = 10
    }
}
