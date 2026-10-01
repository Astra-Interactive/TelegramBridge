package ru.astrainteractive.messagebridge.messenger.discord.connection.event

import kotlinx.coroutines.CompletableDeferred
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.messagebridge.messenger.discord.util.await

internal class JdaShutdownListener : ListenerAdapter() {
    private val closeCode = CompletableDeferred<Int>()

    override fun onShutdown(event: ShutdownEvent) {
        closeCode.complete(event.code)
    }

    suspend fun awaitCloseCode(): Int = closeCode.await()
}
