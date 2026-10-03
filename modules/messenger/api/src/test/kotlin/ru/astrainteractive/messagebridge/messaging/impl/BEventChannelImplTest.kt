@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messaging.impl

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.messagebridge.messaging.api.BEventReceiver
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class BEventChannelImplTest {
    private val chatMessage = Text.Minecraft(
        author = "Steve",
        uuid = "8667ba71-b85a-4004-af54-457a9734eed7",
        text = "hello"
    )

    private fun TestScope.receive(channel: BEventReceiver): List<BEvent> {
        val received = mutableListOf<BEvent>()
        backgroundScope.launch {
            channel.bEvents(backgroundScope).collect { bEvent -> received += bEvent }
        }
        runCurrent()
        return received
    }

    private fun chatMessages(): List<BEvent> {
        return List(EVENT_COUNT) { index -> chatMessage.copy(text = "message $index") }
    }

    @Test
    fun GIVEN_event_consumed_before_any_receiver_WHEN_receiver_subscribes_THEN_it_gets_that_event() = runTest {
        val channel = BEventChannelImpl()
        channel.consume(ServerOpenBEvent)

        val received = receive(channel)
        runCurrent()

        assertEquals(listOf<BEvent>(ServerOpenBEvent), received)
    }

    @Test
    fun GIVEN_subscribed_receiver_WHEN_events_are_consumed_THEN_it_gets_them_in_order() = runTest {
        val channel = BEventChannelImpl()
        val received = receive(channel)

        channel.consume(ServerOpenBEvent)
        channel.consume(chatMessage)
        advanceTimeBy(RECEIVE_WINDOW)
        runCurrent()

        assertEquals(listOf(ServerOpenBEvent, chatMessage), received)
    }

    @Test
    fun GIVEN_two_events_consumed_back_to_back_WHEN_received_THEN_the_second_waits_half_a_second() = runTest {
        val channel = BEventChannelImpl()
        val arrivals = mutableListOf<Duration>()
        backgroundScope.launch {
            channel.bEvents(backgroundScope).collect { _ -> arrivals += currentTime.milliseconds }
        }
        runCurrent()

        channel.consume(ServerOpenBEvent)
        channel.consume(chatMessage)
        advanceTimeBy(RECEIVE_WINDOW)
        runCurrent()

        assertEquals(listOf(Duration.ZERO, 500.milliseconds), arrivals)
    }

    @Test
    fun GIVEN_two_receivers_WHEN_event_is_consumed_THEN_each_gets_it() = runTest {
        val channel = BEventChannelImpl()
        val first = receive(channel)
        val second = receive(channel)

        channel.consume(ServerClosedBEvent)
        runCurrent()

        assertEquals(listOf<BEvent>(ServerClosedBEvent), first)
        assertEquals(listOf<BEvent>(ServerClosedBEvent), second)
    }

    @Test
    fun GIVEN_two_channels_WHEN_event_is_consumed_by_one_THEN_the_other_gets_nothing() = runTest {
        val busy = BEventChannelImpl()
        val idle = BEventChannelImpl()
        val received = receive(idle)

        busy.consume(ServerOpenBEvent)
        advanceTimeBy(RECEIVE_WINDOW)
        runCurrent()

        assertTrue(received.isEmpty())
    }

    @Test
    fun GIVEN_receiver_stuck_on_an_event_WHEN_more_events_than_it_buffers_are_consumed_THEN_others_still_get_them() =
        runTest {
            val channel = BEventChannelImpl()
            backgroundScope.launch {
                channel.bEvents(backgroundScope).collect { _ -> awaitCancellation() }
            }
            val received = receive(channel)
            val events = chatMessages()

            val producer = launch { events.forEach { event -> channel.consume(event) } }
            advanceTimeBy(DELIVERY_WINDOW)
            runCurrent()

            assertTrue(producer.isCompleted)
            assertEquals(events, received)
        }

    @Test
    fun GIVEN_receiver_stuck_on_an_event_WHEN_it_resumes_THEN_it_gets_only_the_newest_buffered_events() = runTest {
        val channel = BEventChannelImpl()
        val gate = CompletableDeferred<Unit>()
        val received = mutableListOf<BEvent>()
        backgroundScope.launch {
            channel.bEvents(backgroundScope).collect { bEvent ->
                gate.await()
                received += bEvent
            }
        }
        runCurrent()
        val events = chatMessages()

        launch { events.forEach { event -> channel.consume(event) } }
        advanceTimeBy(DELIVERY_WINDOW)
        runCurrent()
        gate.complete(Unit)
        runCurrent()

        assertEquals(listOf(events.first()) + events.takeLast(RECEIVER_BUFFER_CAPACITY), received)
    }

    private companion object {
        val RECEIVE_WINDOW = 1000.milliseconds
        const val EVENT_COUNT = 100
        const val RECEIVER_BUFFER_CAPACITY = 64
        val DELIVERY_WINDOW = 500.milliseconds * EVENT_COUNT
    }
}
