@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.discord.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.EventListener
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JdaEventFlowTest {
    private val addedListeners = mutableListOf<EventListener>()
    private val removedListeners = mutableListOf<EventListener>()
    private val jda: JDA = jdaFake(
        mapOf(
            "addEventListener" to JdaAnswer { args -> addedListeners += listenersOf(args) },
            "removeEventListener" to JdaAnswer { args -> removedListeners += listenersOf(args) },
            "getResponseTotal" to 0L
        )
    )

    private fun listenersOf(args: List<Any?>): List<EventListener> {
        return args.first()
            ?.tryCast<Array<*>>()
            .orEmpty()
            .filterIsInstance<EventListener>()
    }

    private fun memberLeft(discordId: Long): GuildMemberRemoveEvent {
        return GuildMemberRemoveEvent(
            jda,
            0,
            jdaFake<Guild>(emptyMap()),
            jdaFake<User>(mapOf("getIdLong" to discordId)),
            null
        )
    }

    private fun TestScope.collectMemberLeaves(onEach: suspend (GuildMemberRemoveEvent) -> Unit): List<Long> {
        val leftIds = mutableListOf<Long>()
        backgroundScope.launch {
            jda.flowEvent<GuildMemberRemoveEvent>()
                .onEach { event -> onEach.invoke(event) }
                .onEach { event -> leftIds += event.user.idLong }
                .collect()
        }
        runCurrent()
        return leftIds
    }

    @Test
    fun GIVEN_collected_flow_WHEN_jda_dispatches_event_of_that_type_THEN_it_is_emitted() = runTest {
        val leftIds = collectMemberLeaves { _ -> }

        addedListeners.single().onEvent(memberLeft(STEVE_ID))
        runCurrent()

        assertEquals(listOf(STEVE_ID), leftIds)
    }

    @Test
    fun GIVEN_collected_flow_WHEN_event_of_another_type_is_dispatched_THEN_nothing_is_emitted() = runTest {
        val leftIds = collectMemberLeaves { _ -> }

        addedListeners.single().onEvent(StatusChangeEvent(jda, JDA.Status.CONNECTED, JDA.Status.LOADING_SUBSYSTEMS))
        runCurrent()

        assertTrue(leftIds.isEmpty())
    }

    @Test
    fun GIVEN_collected_flow_WHEN_collection_is_cancelled_THEN_the_registered_listener_is_removed() = runTest {
        val collection = launch { jda.flowEvent<GuildMemberRemoveEvent>().collect() }
        runCurrent()

        collection.cancel()
        runCurrent()

        assertEquals(addedListeners, removedListeners)
        assertEquals(1, removedListeners.size)
    }

    @Test
    fun GIVEN_collector_that_does_not_keep_up_WHEN_many_events_are_dispatched_THEN_every_event_arrives_later() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val leftIds = collectMemberLeaves { _ -> gate.await() }

            repeat(EVENT_COUNT) { index -> addedListeners.single().onEvent(memberLeft(index.toLong())) }
            gate.complete(Unit)
            runCurrent()

            assertEquals(List(EVENT_COUNT) { index -> index.toLong() }, leftIds)
        }

    private companion object {
        const val STEVE_ID = 4242L
        const val EVENT_COUNT = 100
    }
}
