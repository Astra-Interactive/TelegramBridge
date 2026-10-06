@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.message.internal

import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordMessageSenderTest {
    private var replyRequests = 0
    private val rejectedReply: MessageCreateAction = jdaFake(
        mapOf(
            "queue" to JdaAnswer { args ->
                args[1]
                    ?.tryCast<Consumer<Throwable>>()
                    ?.accept(IllegalStateException("50013: Missing Permissions"))
            }
        )
    )
    private val message: Message = jdaFake(
        mapOf(
            "reply" to JdaAnswer { _ ->
                replyRequests += 1
                rejectedReply
            },
            "getId" to "42"
        )
    )

    @Test
    fun GIVEN_discord_rejects_the_reply_WHEN_replied_THEN_it_is_requested_once_and_reply_returns() = runTest {
        DiscordMessageSender().reply(message, "hello")

        assertEquals(1, replyRequests)
    }
}
