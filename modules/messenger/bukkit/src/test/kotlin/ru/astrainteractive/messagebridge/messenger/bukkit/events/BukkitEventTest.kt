@file:Suppress("FunctionNaming", "DEPRECATION")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.bukkit.events

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.kyori.adventure.text.Component
import org.bukkit.damage.DamageSource
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

private inline fun <reified T : Any> proxyFake(answerByMethod: Map<String, Any?>): T {
    val fake = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        check(method.name in answerByMethod) { "${T::class.simpleName}.${method.name} is not faked" }
        answerByMethod[method.name]
    }
    return fake as T
}

class BukkitEventTest {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = EVENT_BUFFER)
    private val steve: Player = proxyFake(
        mapOf(
            "getName" to "Steve",
            "getUniqueId" to STEVE_UUID,
            "hasPlayedBefore" to true
        )
    )

    private fun TestScope.collect(configuration: PluginConfiguration): List<BEvent> {
        val bukkitEvent = BukkitEvent(
            configKrate = DefaultMutableKrate(factory = { configuration }, loader = { null }).asCachedKrate(),
            eventFlow = { _ -> events }
        )
        val received = mutableListOf<BEvent>()
        backgroundScope.launch {
            bukkitEvent.bEvents
                .onEach { bEvent -> received += bEvent }
                .collect()
        }
        runCurrent()
        return received
    }

    private fun TestScope.dispatch(event: Event) {
        events.tryEmit(event)
        runCurrent()
    }

    private fun chat(message: String, isCancelled: Boolean): AsyncPlayerChatEvent {
        return AsyncPlayerChatEvent(true, steve, message, emptySet()).apply { this.isCancelled = isCancelled }
    }

    private fun death(isCancelled: Boolean): PlayerDeathEvent {
        val damageSource: DamageSource = proxyFake(emptyMap())
        return PlayerDeathEvent(steve, damageSource, emptyList(), 0, "Steve fell from a high place")
            .apply { this.isCancelled = isCancelled }
    }

    @Test
    fun GIVEN_join_messages_enabled_WHEN_player_joins_THEN_joined_event_with_played_before_is_emitted() = runTest {
        val received = collect(PluginConfiguration())

        dispatch(PlayerJoinEvent(steve, Component.empty()))

        val joined = PlayerJoinedBEvent(name = "Steve", uuid = "$STEVE_UUID", hasPlayedBefore = true)
        assertEquals(listOf<BEvent>(joined), received)
    }

    @Test
    fun GIVEN_join_messages_disabled_WHEN_player_joins_THEN_nothing_is_emitted() = runTest {
        val received = collect(PluginConfiguration(displayJoinMessage = false))

        dispatch(PlayerJoinEvent(steve, Component.empty()))

        assertEquals(emptyList(), received)
    }

    @Test
    fun GIVEN_leave_messages_enabled_WHEN_player_quits_THEN_leave_event_is_emitted() = runTest {
        val received = collect(PluginConfiguration())

        dispatch(PlayerQuitEvent(steve, Component.empty()))

        assertEquals(listOf<BEvent>(PlayerLeaveBEvent(name = "Steve", uuid = "$STEVE_UUID")), received)
    }

    @Test
    fun GIVEN_leave_messages_disabled_WHEN_player_quits_THEN_nothing_is_emitted() = runTest {
        val received = collect(PluginConfiguration(displayLeaveMessage = false))

        dispatch(PlayerQuitEvent(steve, Component.empty()))

        assertEquals(emptyList(), received)
    }

    @Test
    fun GIVEN_chat_message_WHEN_it_arrives_THEN_minecraft_text_with_that_message_is_emitted() = runTest {
        val received = collect(PluginConfiguration())

        dispatch(chat(message = "hello", isCancelled = false))

        assertEquals(listOf<BEvent>(Text.Minecraft(author = "Steve", uuid = "$STEVE_UUID", text = "hello")), received)
    }

    @Test
    fun GIVEN_chat_cancelled_by_another_plugin_WHEN_it_arrives_THEN_nothing_is_emitted() = runTest {
        val received = collect(PluginConfiguration())

        dispatch(chat(message = "muted words", isCancelled = true))

        assertEquals(emptyList(), received)
    }

    @Test
    fun GIVEN_death_messages_enabled_WHEN_player_dies_THEN_death_event_with_its_message_is_emitted() = runTest {
        val received = collect(PluginConfiguration())

        dispatch(death(isCancelled = false))

        assertEquals(
            listOf<BEvent>(
                PlayerDeathBEvent(name = "Steve", uuid = "$STEVE_UUID", cause = "Steve fell from a high place")
            ),
            received
        )
    }

    @Test
    fun GIVEN_death_messages_disabled_WHEN_player_dies_THEN_nothing_is_emitted() = runTest {
        val received = collect(PluginConfiguration(displayDeathMessage = false))

        dispatch(death(isCancelled = false))

        assertEquals(emptyList(), received)
    }

    @Test
    fun GIVEN_death_cancelled_by_another_plugin_WHEN_it_arrives_THEN_nothing_is_emitted() = runTest {
        val received = collect(PluginConfiguration())

        dispatch(death(isCancelled = true))

        assertEquals(emptyList(), received)
    }

    private companion object {
        val STEVE_UUID: UUID = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7")
        const val EVENT_BUFFER = 8
    }
}
