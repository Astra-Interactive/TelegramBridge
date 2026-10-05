package ru.astrainteractive.messagebridge.messenger.discord.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.IEventManager
import ru.astrainteractive.klibs.mikro.core.util.tryCast

internal inline fun <reified T : GenericEvent> IEventManager.flowEvent(): Flow<T> = callbackFlow {
    val listener = EventListener { event ->
        event.tryCast<T>()?.let { typedEvent -> trySend(typedEvent) }
    }
    register(listener)
    awaitClose { unregister(listener) }
}.buffer(Channel.UNLIMITED)
