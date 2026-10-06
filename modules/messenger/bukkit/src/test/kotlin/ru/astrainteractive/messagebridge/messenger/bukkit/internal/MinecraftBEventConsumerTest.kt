@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.bukkit.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.api.TextInterceptor
import ru.astrainteractive.messagebridge.messenger.api.fake.FakeBEventReceiver
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import kotlin.test.Test
import kotlin.test.assertEquals

private object UnusedDispatchers : KotlinDispatchers {
    override val Main: MainCoroutineDispatcher
        get() = error("Formatting a message never switches threads")
    override val IO: CoroutineDispatcher
        get() = error("Formatting a message never switches threads")
    override val Default: CoroutineDispatcher
        get() = error("Formatting a message never switches threads")
    override val Unconfined: CoroutineDispatcher
        get() = error("Formatting a message never switches threads")
}

class MinecraftBEventConsumerTest {
    private val translationKrate = DefaultMutableKrate(factory = { PluginTranslation() }, loader = { null })
        .asCachedKrate()
    private val telegramText = Text.Telegram(
        author = "steve_tg",
        text = "hello",
        authorId = STEVE_TG,
        reply = Text.Reply(author = "alex_tg", authorId = ALEX_TG, text = "hi", target = null),
        ref = TG_REF
    )

    private fun consumer(vararg interceptors: TextInterceptor): MinecraftBEventConsumer {
        return MinecraftBEventConsumer(
            translationKrate = translationKrate,
            textInterceptors = interceptors.toList(),
            dispatchers = UnusedDispatchers,
            bEventReceiver = FakeBEventReceiver(emptyFlow())
        )
    }

    private fun plainText(component: LocalizableComponent): String {
        return PlainTextComponentSerializer.plainText().serialize(component.toComponent(MinecraftLocales.EN_US))
    }

    @Test
    fun GIVEN_interceptor_that_renames_author_and_reply_author_WHEN_shown_in_game_THEN_line_has_new_names() =
        runTest {
            val renamed = telegramText.copy(
                author = "Steve",
                reply = Text.Reply(author = "Alex", authorId = ALEX_TG, text = "hi", target = null)
            )

            val component = consumer(TextInterceptor { _ -> renamed }).toMinecraftComponent(telegramText)

            assertEquals("[TG] Steve ↪ Alex: hello", plainText(component))
        }

    @Test
    fun GIVEN_no_interceptors_WHEN_shown_in_game_THEN_line_has_names_from_the_message() = runTest {
        val component = consumer().toMinecraftComponent(telegramText)

        assertEquals("[TG] steve_tg ↪ alex_tg: hello", plainText(component))
    }

    @Test
    fun GIVEN_two_interceptors_WHEN_shown_in_game_THEN_second_gets_what_first_returned() = runTest {
        val renamedAuthor = telegramText.copy(author = "Steve")
        val receivedBySecond = mutableListOf<Text>()
        val second = TextInterceptor { text ->
            receivedBySecond += text
            text
        }

        val component = consumer(TextInterceptor { _ -> renamedAuthor }, second).toMinecraftComponent(telegramText)

        assertEquals(listOf<Text>(renamedAuthor), receivedBySecond)
        assertEquals("[TG] Steve ↪ alex_tg: hello", plainText(component))
    }

    @Test
    fun GIVEN_message_without_reply_WHEN_shown_in_game_THEN_line_has_no_reply_marker() = runTest {
        val discordText = Text.Discord(
            author = "Stevie",
            text = "hello",
            authorId = STEVE_DS,
            reply = null,
            ref = DS_REF
        )

        val component = consumer().toMinecraftComponent(discordText)

        assertEquals("[DS] Stevie: hello", plainText(component))
    }

    @Test
    fun GIVEN_main_thread_unavailable_WHEN_chat_message_is_consumed_THEN_consume_returns() = runTest {
        consumer().consume(telegramText)
    }

    private companion object {
        val TG_REF = MessageRef.Telegram(chatId = -1001L, messageId = 1)
        val DS_REF = MessageRef.Discord(messageId = 1L)
        const val STEVE_TG = 77L
        const val ALEX_TG = 78L
        const val STEVE_DS = 4242L
    }
}
