@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.command

import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.model.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.model.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.link.code.memory.InMemoryLinkCodeRepository
import ru.astrainteractive.messagebridge.link.code.model.CodeUser
import ru.astrainteractive.messagebridge.link.fake.FakeLinkedPlayerRepository
import ru.astrainteractive.messagebridge.link.fake.FakeLuckPermsProvider
import ru.astrainteractive.messagebridge.link.model.MessengerAccount
import ru.astrainteractive.messagebridge.link.role.luckperms.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.usecase.LinkAccountUseCase
import ru.astrainteractive.messagebridge.messenger.api.model.Interception
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelegramLinkInterceptorTest {
    private val translation = PluginTranslation()
    private val configKrate = DefaultMutableKrate(factory = { PluginConfiguration() }, loader = { null })
        .asCachedKrate()
    private val linkCodeRepository = InMemoryLinkCodeRepository()
    private val linkedPlayerRepository = FakeLinkedPlayerRepository()
    private val interceptor = TelegramLinkInterceptor(
        linkAccountUseCase = LinkAccountUseCase(
            linkCodeRepository = linkCodeRepository,
            linkedPlayerRepository = linkedPlayerRepository,
            luckPermsRoleController = LuckPermsRoleController(
                configKrate = configKrate,
                luckPermsProvider = FakeLuckPermsProvider()
            )
        ),
        translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
    )
    private val steve = CodeUser(name = "Steve", uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"))
    private val telegramSteve = User(TELEGRAM_ID, "Steve", false).apply { userName = "steve_tg" }

    private fun update(text: String, from: User?): Update = Update().apply {
        message = Message().apply {
            this.text = text
            this.from = from
        }
    }

    private fun reply(text: LocalizableComponent): Interception = Interception.Reply(text.toMessengerText())

    @Test
    fun GIVEN_update_without_message_WHEN_intercepted_THEN_it_passes() = runTest {
        assertEquals(Interception.Pass, interceptor.intercept(Update()))
    }

    @Test
    fun GIVEN_ordinary_chat_text_WHEN_intercepted_THEN_it_passes_and_nothing_is_linked() = runTest {
        linkCodeRepository.generateCodeForPlayer(steve)

        val interception = interceptor.intercept(update(text = "hello", from = telegramSteve))

        assertEquals(Interception.Pass, interception)
        assertTrue(linkedPlayerRepository.linkedPlayers.isEmpty())
    }

    @Test
    fun GIVEN_code_created_in_game_WHEN_user_sends_it_THEN_telegram_is_linked_and_user_reads_success() = runTest {
        val code = linkCodeRepository.generateCodeForPlayer(steve)

        val interception = interceptor.intercept(update(text = "/link $code", from = telegramSteve))

        assertEquals(reply(translation.link.success), interception)
        assertEquals(
            MessengerAccount.Telegram(id = TELEGRAM_ID, username = "steve_tg"),
            linkedPlayerRepository.linkedPlayers[steve.uuid]?.telegram
        )
    }

    @Test
    fun GIVEN_code_already_redeemed_WHEN_sent_again_THEN_user_reads_no_code_found() = runTest {
        val code = linkCodeRepository.generateCodeForPlayer(steve)
        interceptor.intercept(update(text = "/link $code", from = telegramSteve))

        val interception = interceptor.intercept(update(text = "/link $code", from = telegramSteve))

        assertEquals(reply(translation.link.noCodeFound), interception)
    }

    @Test
    fun GIVEN_code_nobody_created_WHEN_sent_THEN_user_reads_no_code_found_and_nothing_is_linked() = runTest {
        val interception = interceptor.intercept(update(text = "/link 1234", from = telegramSteve))

        assertEquals(reply(translation.link.noCodeFound), interception)
        assertTrue(linkedPlayerRepository.linkedPlayers.isEmpty())
    }

    @Test
    fun GIVEN_link_command_without_a_number_WHEN_sent_THEN_user_reads_no_code_found() = runTest {
        linkCodeRepository.generateCodeForPlayer(steve)

        val interception = interceptor.intercept(update(text = "/link abc", from = telegramSteve))

        assertEquals(reply(translation.link.noCodeFound), interception)
        assertTrue(linkedPlayerRepository.linkedPlayers.isEmpty())
    }

    @Test
    fun GIVEN_user_without_username_WHEN_sends_code_THEN_reads_no_username_and_code_still_links_later() = runTest {
        val code = linkCodeRepository.generateCodeForPlayer(steve)
        val nameless = User(TELEGRAM_ID, "Steve", false)

        val interception = interceptor.intercept(update(text = "/link $code", from = nameless))

        assertEquals(reply(translation.link.noUsername), interception)
        assertEquals(
            reply(translation.link.success),
            interceptor.intercept(update(text = "/link $code", from = telegramSteve))
        )
    }

    @Test
    fun GIVEN_message_without_sender_WHEN_link_is_sent_THEN_it_is_consumed_and_code_stays_valid() = runTest {
        val code = linkCodeRepository.generateCodeForPlayer(steve)

        val interception = interceptor.intercept(update(text = "/link $code", from = null))

        assertEquals(Interception.Consumed, interception)
        assertTrue(linkedPlayerRepository.linkedPlayers.isEmpty())
        assertEquals(steve, linkCodeRepository.findUserByCode(code))
    }

    @Test
    fun GIVEN_player_already_linked_to_telegram_WHEN_sends_new_code_THEN_reads_already_linked_and_link_stays() =
        runTest {
            val existingLink = MessengerAccount.Telegram(id = 1, username = "old_tg")
            linkedPlayerRepository.link(uuid = steve.uuid, minecraftName = "Steve", account = existingLink)
            val code = linkCodeRepository.generateCodeForPlayer(steve)

            val interception = interceptor.intercept(update(text = "/link $code", from = telegramSteve))

            assertEquals(reply(translation.link.alreadyLinked), interception)
            assertEquals(existingLink, linkedPlayerRepository.linkedPlayers[steve.uuid]?.telegram)
        }

    @Test
    fun GIVEN_unreadable_database_WHEN_code_is_sent_THEN_user_reads_unknown_error() = runTest {
        val code = linkCodeRepository.generateCodeForPlayer(steve)
        linkedPlayerRepository.linkFailure = IllegalStateException("Database is locked")

        val interception = interceptor.intercept(update(text = "/link $code", from = telegramSteve))

        assertEquals(reply(translation.link.unknownError), interception)
        assertTrue(linkedPlayerRepository.linkedPlayers.isEmpty())
    }

    @Test
    fun GIVEN_telegram_account_linked_to_another_player_WHEN_code_is_sent_THEN_user_reads_account_taken() = runTest {
        val alexUuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000003")
        linkedPlayerRepository.link(
            uuid = alexUuid,
            minecraftName = "Alex",
            account = MessengerAccount.Telegram(TELEGRAM_ID, "steve_tg")
        )
        val code = linkCodeRepository.generateCodeForPlayer(steve)

        val interception = interceptor.intercept(update(text = "/link $code", from = telegramSteve))

        assertEquals(reply(translation.link.accountTaken), interception)
        assertEquals(null, linkedPlayerRepository.linkedPlayers[steve.uuid])
    }

    private companion object {
        const val TELEGRAM_ID = 77L
    }
}
