package ru.astrainteractive.messagebridge.forge.core.event

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.EventPriority
import java.util.function.Consumer

fun <T : Event> flowEvent(
    type: Class<T>,
    priority: EventPriority = EventPriority.NORMAL
) = callbackFlow {
    val isCancelled = false
    val consumer = Consumer {
        launch { send(it) }
    }
    MinecraftForge.EVENT_BUS.addListener<T>(
        priority,
        isCancelled,
        type,
        consumer
    )
    awaitClose {
        MinecraftForge.EVENT_BUS.unregister(consumer)
    }
}

inline fun <reified T : Event> flowEvent(
    priority: EventPriority = EventPriority.NORMAL
) = flowEvent(
    type = T::class.java,
    priority = priority
)
