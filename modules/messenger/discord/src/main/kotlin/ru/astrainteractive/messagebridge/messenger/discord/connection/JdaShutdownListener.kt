package ru.astrainteractive.messagebridge.messenger.discord.connection

import kotlinx.coroutines.CompletableDeferred
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

/**
 * Remembers the close code JDA stopped with. `awaitReady` only throws that JDA was shut down, while the code tells
 * a disabled intent (4014) from a reset token (4004).
 */
internal class JdaShutdownListener : ListenerAdapter() {
    private val closeCode = CompletableDeferred<Int>()

    override fun onShutdown(event: ShutdownEvent) {
        closeCode.complete(event.code)
    }

    suspend fun awaitCloseCode(): Int = closeCode.await()
}
