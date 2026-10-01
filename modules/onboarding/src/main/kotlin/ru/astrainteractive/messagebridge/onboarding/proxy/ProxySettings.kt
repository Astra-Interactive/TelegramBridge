package ru.astrainteractive.messagebridge.onboarding.proxy

import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.ProxyType
import ru.astrainteractive.messagebridge.onboarding.input.refuse
import ru.astrainteractive.messagebridge.onboarding.messenger.Messenger
import ru.astrainteractive.messagebridge.onboarding.secret.SecretGuard
import ru.astrainteractive.messagebridge.onboarding.secret.SecretInput
import ru.astrainteractive.messagebridge.onboarding.setting.Setting

/** Reads `proxy <type> <host> <port> [username] [password] [--unsafe]` into the proxy it sets. */
internal class ProxySettings(
    private val secretGuard: SecretGuard,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate

    /** @return the username and the password, which is a secret and may come only with [SecretInput.UNSAFE_FLAG] */
    private fun credentialsOf(sender: KCommandSender, credentials: String): Result<List<String>> {
        val input = SecretInput.parse(credentials)
        return when {
            input.words.size > MAX_CREDENTIALS -> refuse(translation.setup.invalidProxyCredentials)
            input.words.size == MAX_CREDENTIALS -> secretGuard.allow(sender, input).map { allowed -> allowed.words }
            else -> Result.success(input.words)
        }
    }

    private fun hostOf(value: String): Result<String> {
        if (!PROXY_HOST.matches(value)) return refuse(translation.setup.invalidHost)
        return Result.success(value)
    }

    private fun portOf(value: String): Result<Int> {
        val port = value.toIntOrNull()?.takeIf { number -> number in MIN_PORT..MAX_PORT }
            ?: return refuse(translation.setup.invalidPort)
        return Result.success(port)
    }

    /** @param credentials what is typed after the port: nothing, the username, or the username and the password */
    fun parse(
        sender: KCommandSender,
        type: ProxyType,
        host: String,
        port: String,
        credentials: String
    ): Result<PluginConfiguration.Proxy> {
        val words = credentialsOf(sender, credentials).getOrElse { error -> return Result.failure(error) }
        val validHost = hostOf(host).getOrElse { error -> return Result.failure(error) }
        val validPort = portOf(port).getOrElse { error -> return Result.failure(error) }
        val proxy = PluginConfiguration.Proxy(
            type = type,
            host = validHost,
            port = validPort,
            username = words.getOrNull(0),
            password = words.getOrNull(1)
        )
        return Result.success(proxy)
    }

    /** @param proxy `null` removes the proxy of [messenger] */
    fun <C> settingOf(messenger: Messenger<C>, proxy: PluginConfiguration.Proxy?): Setting<C> {
        return Setting<C>(saved = translation.setup.saved.proxyOf(proxy)) { section ->
            messenger.withProxy(section, proxy)
        }
    }

    private companion object {
        const val MIN_PORT = 1
        const val MAX_PORT = 65535
        const val MAX_CREDENTIALS = 2
        val PROXY_HOST = Regex("^[A-Za-z0-9.-]+$|^[0-9A-Fa-f:]+$")
    }
}
