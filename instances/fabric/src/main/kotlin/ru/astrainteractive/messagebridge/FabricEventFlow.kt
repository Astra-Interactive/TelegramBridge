package ru.astrainteractive.messagebridge

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy


// data
data class KFabricEventData1<T>(val t: T)
data class KFabricEventData2<T1, T2>(val t1: T1, val t2: T2)
data class KFabricEventData3<T1, T2, T3>(val t1: T1, val t2: T2, val t3: T3)

// scope
interface FabricEventProducerScope<in E> : ProducerScope<E>
class FabricEventProducerScopeImpl<in E>(
    instance: ProducerScope<E>
) : FabricEventProducerScope<E>, ProducerScope<E> by instance

fun <T> fabricEventFlow(block: suspend FabricEventProducerScope<T>.() -> Unit): Flow<T> = channelFlow {
    val scope = FabricEventProducerScopeImpl(this)
    block.invoke(scope)
}

inline fun <reified T> FabricEventProducerScope<KFabricEventData1<T>>.send(t: T) {
    launch { send(KFabricEventData1(t)) }
}

fun <T1, T2> FabricEventProducerScope<KFabricEventData2<T1, T2>>.send(t1: T1, t2: T2) {
    launch { send(KFabricEventData2(t1, t2)) }
}

fun <T1, T2, T3> FabricEventProducerScope<KFabricEventData3<T1, T2, T3>>.send(t1: T1, t2: T2, t3: T3) {
    launch { send(KFabricEventData3(t1, t2, t3)) }
}

// samples
val START_SERVER_TICK = fabricEventFlow {
    val callback = ServerTickEvents.StartTick(::send)
    ServerTickEvents.START_SERVER_TICK.register(callback)
}

val END_SERVER_TICK = fabricEventFlow {
    val callback = ServerTickEvents.EndTick(::send)
    ServerTickEvents.END_SERVER_TICK.register(callback)
}

val JOIN = fabricEventFlow {
    val callback = ServerPlayConnectionEvents.Join(::send)
    ServerPlayConnectionEvents.JOIN.register(callback)
}
