@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberLeft
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberRestricted
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramRequestResult
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.BOT_USER_NAME
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.CHAT_ID
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.userOf
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.model.Check
import ru.astrainteractive.messagebridge.onboarding.api.model.CheckLevel
import ru.astrainteractive.messagebridge.onboarding.telegram.fake.ShownCheck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelegramDiagnosticsTest {
    private val translation = OnboardingTranslation()
    private val check = translation.telegram.check
    private val translationKrate = FakeTranslationKrate(translation)
    private val failureTextMapper = FakeTelegramFailureTextMapper()
    private val botApi = FakeTelegramBotApi()
    private val ready = TelegramConnectionState.Connected(botName = "@$BOT_USER_NAME")
    private val bot = userOf(id = 1L, firstName = "Bridge", userName = BOT_USER_NAME, isBot = true).apply {
        canReadAllGroupMessages = false
    }

    @Volatile
    private var chat: ChatFullInfo = chatInfoOf(isForum = false)

    @Volatile
    private var member: ChatMember = ChatMemberAdministrator().apply { canDeleteMessages = true }

    @Volatile
    private var failingMethod: String? = null

    private fun chatInfoOf(type: String = "supergroup", isForum: Boolean): ChatFullInfo = ChatFullInfo.builder()
        .id(CHAT_ID)
        .type(type)
        .title("Server chat")
        .isForum(isForum)
        .build()

    private fun answerOf(method: BotApiMethod<*>): TelegramRequestResult<*> {
        if (method.method == failingMethod) return TelegramRequestResult.Failed(TelegramFailure.ChatNotFound)
        return when (method) {
            is GetMe -> TelegramRequestResult.Success(bot)
            is GetChat -> TelegramRequestResult.Success(chat)
            is GetChatMember -> TelegramRequestResult.Success(member)
            is SendMessage -> TelegramRequestResult.Success(Message())
            else -> error("Unexpected ${method.method}")
        }
    }

    private fun diagnosticsOf(
        connectionState: TelegramConnectionState,
        configuration: PluginConfiguration = configurationOf()
    ): TelegramDiagnostics {
        botApi.answer = { method -> answerOf(method) }
        return TelegramDiagnostics(
            configFlow = MutableStateFlow(configuration),
            translationKrate = translationKrate,
            connectionState = MutableStateFlow(connectionState),
            botApi = botApi,
            failureTextMapper = failureTextMapper
        )
    }

    private fun checkOf(level: CheckLevel, message: LocalizableComponent) = ShownCheck(level, message.toMessengerText())

    private fun List<Check>.shown() = map { item -> ShownCheck(item.level, item.message.toMessengerText()) }

    @Test
    fun GIVEN_no_token_WHEN_checked_THEN_it_says_how_to_get_one() = runTest {
        val checks = diagnosticsOf(TelegramConnectionState.Disabled).diagnose()

        assertEquals(listOf(checkOf(CheckLevel.ERROR, check.tokenMissing)), checks.shown())
    }

    @Test
    fun GIVEN_settings_that_cannot_work_WHEN_checked_THEN_it_says_why() = runTest {
        val checks = diagnosticsOf(TelegramConnectionState.Failed(TelegramFailure.InvalidApiUrl)).diagnose()

        val reason = failureTextMapper.map(TelegramFailure.InvalidApiUrl)
        assertEquals(listOf(checkOf(CheckLevel.ERROR, reason)), checks.shown())
    }

    @Test
    fun GIVEN_revoked_token_WHEN_checked_THEN_check_stops_at_the_token() = runTest {
        val diagnostics = diagnosticsOf(ready)
        botApi.answer = { _ -> TelegramRequestResult.Failed(TelegramFailure.InvalidToken) }

        val checks = diagnostics.diagnose()

        val reason = failureTextMapper.map(TelegramFailure.InvalidToken)
        assertEquals(listOf(checkOf(CheckLevel.ERROR, reason)), checks.shown())
    }

    @Test
    fun GIVEN_bot_reconnecting_WHEN_checked_THEN_it_asks_to_check_again() = runTest {
        val diagnostics = diagnosticsOf(ready)
        botApi.answer = { _ -> TelegramRequestResult.NotConnected }

        val checks = diagnostics.diagnose()

        assertEquals(listOf(checkOf(CheckLevel.ERROR, check.reconnecting)), checks.shown())
    }

    @Test
    fun GIVEN_no_chat_WHEN_checked_THEN_it_says_how_to_bind_one() = runTest {
        val checks = diagnosticsOf(ready, configurationOf(chatId = "")).diagnose()

        assertEquals(
            listOf(
                checkOf(CheckLevel.OK, check.botWorks("@$BOT_USER_NAME")),
                checkOf(CheckLevel.ERROR, failureTextMapper.map(TelegramFailure.ChatNotSet))
            ),
            checks.shown()
        )
    }

    @Test
    fun GIVEN_chat_the_bot_cannot_see_WHEN_checked_THEN_check_stops_at_the_chat() = runTest {
        failingMethod = GetChat.PATH

        val checks = diagnosticsOf(ready).diagnose()

        val reason = failureTextMapper.map(TelegramFailure.ChatNotFound)
        assertEquals(checkOf(CheckLevel.ERROR, reason), checks.shown().last())
        assertEquals(2, checks.size)
    }

    @Test
    fun GIVEN_everything_set_up_WHEN_checked_THEN_every_step_passes_and_a_test_message_is_sent() = runTest {
        val checks = diagnosticsOf(ready).diagnose()

        assertEquals(
            listOf(
                checkOf(CheckLevel.OK, check.botWorks("@$BOT_USER_NAME")),
                checkOf(CheckLevel.OK, check.chatFound(title = "Server chat", type = "supergroup", isForum = false)),
                checkOf(CheckLevel.OK, check.botAdmin),
                checkOf(CheckLevel.OK, check.testMessageSent)
            ),
            checks.shown()
        )
        assertEquals(check.testMessage.toMessengerText(), botApi.sentMessages.single().text)
    }

    @Test
    fun GIVEN_bound_topic_WHEN_checked_THEN_test_message_goes_into_it() = runTest {
        diagnosticsOf(ready, configurationOf(topicId = "12")).diagnose()

        assertEquals(12, botApi.sentMessages.single().replyToMessageId)
    }

    @Test
    fun GIVEN_owner_bot_WHEN_checked_THEN_it_is_an_admin() = runTest {
        member = ChatMemberOwner()

        val checks = diagnosticsOf(ready).diagnose()

        assertTrue(checkOf(CheckLevel.OK, check.botAdmin) in checks.shown())
    }

    @Test
    fun GIVEN_admin_that_cannot_delete_WHEN_checked_THEN_it_warns_about_long_messages() = runTest {
        member = ChatMemberAdministrator().apply { canDeleteMessages = false }

        val checks = diagnosticsOf(ready).diagnose()

        assertTrue(checkOf(CheckLevel.WARNING, check.botCantDelete) in checks.shown())
    }

    @Test
    fun GIVEN_bot_removed_from_chat_WHEN_checked_THEN_it_is_not_in_chat() = runTest {
        member = ChatMemberLeft()

        val checks = diagnosticsOf(ready).diagnose()

        assertTrue(checkOf(CheckLevel.ERROR, failureTextMapper.map(TelegramFailure.BotNotInChat)) in checks.shown())
    }

    @Test
    fun GIVEN_muted_bot_WHEN_checked_THEN_it_has_no_rights() = runTest {
        member = ChatMemberRestricted().apply { canSendMessages = false }

        val checks = diagnosticsOf(ready).diagnose()

        assertTrue(checkOf(CheckLevel.ERROR, failureTextMapper.map(TelegramFailure.NoRights)) in checks.shown())
    }

    @Test
    fun GIVEN_plain_member_with_privacy_on_WHEN_checked_THEN_it_warns_and_explains_privacy() = runTest {
        member = ChatMemberMember()

        val checks = diagnosticsOf(ready).diagnose().shown()

        assertTrue(checkOf(CheckLevel.WARNING, check.botNotAdmin) in checks)
        assertTrue(checkOf(CheckLevel.ERROR, check.privacyMode) in checks)
    }

    @Test
    fun GIVEN_plain_member_that_reads_all_messages_WHEN_checked_THEN_only_admin_rights_are_missing() = runTest {
        member = ChatMemberRestricted().apply { canSendMessages = true }
        bot.canReadAllGroupMessages = true

        val checks = diagnosticsOf(ready).diagnose().shown()

        assertTrue(checkOf(CheckLevel.WARNING, check.botNotAdmin) in checks)
        assertTrue(checks.none { item -> item == checkOf(CheckLevel.ERROR, check.privacyMode) })
    }

    @Test
    fun GIVEN_private_chat_WHEN_checked_THEN_membership_is_not_checked() = runTest {
        chat = chatInfoOf(type = "private", isForum = false)

        diagnosticsOf(ready).diagnose()

        assertTrue(botApi.requests.none { method -> method is GetChatMember })
    }

    @Test
    fun GIVEN_forum_without_topic_WHEN_checked_THEN_it_warns_messages_go_to_general() = runTest {
        chat = chatInfoOf(isForum = true)

        val checks = diagnosticsOf(ready).diagnose().shown()

        assertTrue(checkOf(CheckLevel.WARNING, check.forumWithoutTopic) in checks)
    }

    @Test
    fun GIVEN_topic_in_chat_without_topics_WHEN_checked_THEN_it_warns_about_the_reply_thread() = runTest {
        val checks = diagnosticsOf(ready, configurationOf(topicId = "12")).diagnose().shown()

        assertTrue(checkOf(CheckLevel.WARNING, check.replyThread("12")) in checks)
    }

    @Test
    fun GIVEN_test_message_rejected_WHEN_checked_THEN_last_check_says_why() = runTest {
        failingMethod = SendMessage.PATH

        val checks = diagnosticsOf(ready).diagnose()

        val reason = failureTextMapper.map(TelegramFailure.ChatNotFound)
        assertEquals(checkOf(CheckLevel.ERROR, reason), checks.shown().last())
    }
}
