@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.neoforge.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.config.PluginTranslation
import ru.astrainteractive.messagebridge.messaging.fake.FakeBEventReceiver
import ru.astrainteractive.messagebridge.messaging.model.Text
import kotlin.test.Test

private object UnusedDispatchers : KotlinDispatchers {
    override val Main: MainCoroutineDispatcher
        get() = error("The server main thread is unavailable")
    override val IO: CoroutineDispatcher
        get() = error("Showing a message never switches to IO")
    override val Default: CoroutineDispatcher
        get() = error("Showing a message never switches to Default")
    override val Unconfined: CoroutineDispatcher
        get() = error("Showing a message never switches to Unconfined")
}

class NeoForgeBEventConsumerTest {
    private val translationKrate = DefaultMutableKrate(factory = { PluginTranslation() }, loader = { null })
        .asCachedKrate()
    private val telegramText = Text.Telegram(
        author = "steve_tg",
        text = "hello",
        authorId = STEVE_TG,
        reply = null
    )

    private fun consumer(): NeoForgeBEventConsumer {
        return NeoForgeBEventConsumer(
            translationKrate = translationKrate,
            dispatchers = UnusedDispatchers,
            bEventReceiver = FakeBEventReceiver(emptyFlow())
        )
    }

    @Test
    fun GIVEN_main_thread_unavailable_WHEN_chat_message_is_consumed_THEN_consume_returns() = runTest {
        consumer().consume(telegramText)
    }

    private companion object {
        const val STEVE_TG = 77L
    }
}
