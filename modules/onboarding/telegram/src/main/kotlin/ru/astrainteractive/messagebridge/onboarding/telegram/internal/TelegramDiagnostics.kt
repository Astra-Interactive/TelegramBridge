package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import kotlinx.coroutines.flow.StateFlow
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberLeft
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberRestricted
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramRequestResult
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.model.Check
import ru.astrainteractive.messagebridge.onboarding.api.model.CheckLevel

internal class TelegramDiagnostics(
    private val configFlow: StateFlow<PluginConfiguration>,
    translationKrate: CachedKrate<OnboardingTranslation>,
    private val connectionState: StateFlow<TelegramConnectionState>,
    private val botApi: TelegramBotApi,
    private val failureTextMapper: TelegramFailureTextMapper,
) {
    private val translation by translationKrate

    private fun okCheck(message: LocalizableComponent) = Check(CheckLevel.OK, message)

    private fun warningCheck(message: LocalizableComponent) = Check(CheckLevel.WARNING, message)

    private fun errorCheck(message: LocalizableComponent) = Check(CheckLevel.ERROR, message)

    private inline fun <T> TelegramRequestResult<T>.valueOrElse(onFailure: (Check) -> Nothing): T = when (this) {
        is TelegramRequestResult.Success -> value
        TelegramRequestResult.NotConnected -> onFailure(errorCheck(translation.telegram.check.reconnecting))
        is TelegramRequestResult.Failed -> onFailure(errorCheck(failureTextMapper.map(failure)))
    }

    private fun notAdminChecks(bot: User): List<Check> {
        val check = translation.telegram.check
        return listOfNotNull(
            warningCheck(check.botNotAdmin),
            errorCheck(check.privacyMode).takeIf { _ -> bot.canReadAllGroupMessages != true }
        )
    }

    private suspend fun memberChecks(chat: ChatFullInfo, bot: User): List<Check> {
        if (chat.isGroupChat != true && chat.isSuperGroupChat != true) return emptyList()
        val check = translation.telegram.check
        val member = botApi.execute(GetChatMember(chat.id.toString(), bot.id))
            .valueOrElse { failed -> return listOf(failed) }
        return when (member) {
            is ChatMemberOwner -> listOf(okCheck(check.botAdmin))
            is ChatMemberAdministrator -> listOfNotNull(
                okCheck(check.botAdmin),
                warningCheck(check.botCantDelete).takeIf { _ -> member.canDeleteMessages != true }
            )

            is ChatMemberLeft, is ChatMemberBanned -> {
                listOf(errorCheck(failureTextMapper.map(TelegramFailure.BotNotInChat)))
            }
            is ChatMemberRestricted -> if (member.canSendMessages == false) {
                listOf(errorCheck(failureTextMapper.map(TelegramFailure.NoRights)))
            } else {
                notAdminChecks(bot)
            }

            else -> notAdminChecks(bot)
        }
    }

    private fun topicChecks(chat: ChatFullInfo, topicId: String): List<Check> {
        val check = translation.telegram.check
        val isForum = chat.isForum == true
        return when {
            isForum && topicId.isBlank() -> listOf(warningCheck(check.forumWithoutTopic))
            !isForum && topicId.isNotBlank() -> listOf(warningCheck(check.replyThread(topicId)))
            else -> emptyList()
        }
    }

    private suspend fun deliveryCheck(tgConfig: PluginConfiguration.TelegramConfig): Check {
        val check = translation.telegram.check
        val sendMessage = SendMessage(tgConfig.chatID, check.testMessage.toMessengerText()).apply {
            replyToMessageId = tgConfig.topicID.toIntOrNull()
        }
        botApi.execute(sendMessage).valueOrElse { failed -> return failed }
        return okCheck(check.testMessageSent)
    }

    private suspend fun chatChecks(bot: User): List<Check> {
        val tgConfig = configFlow.value.tgConfig
        if (tgConfig.chatID.isBlank()) return listOf(errorCheck(failureTextMapper.map(TelegramFailure.ChatNotSet)))
        val chat = botApi.execute(GetChat(tgConfig.chatID)).valueOrElse { failed -> return listOf(failed) }
        val chatCheck = okCheck(
            translation.telegram.check.chatFound(
                title = chat.title ?: chat.userName ?: tgConfig.chatID,
                type = chat.type.orEmpty(),
                isForum = chat.isForum == true
            )
        )
        return listOf(chatCheck) +
            memberChecks(chat, bot) +
            topicChecks(chat, tgConfig.topicID) +
            deliveryCheck(tgConfig)
    }

    suspend fun diagnose(): List<Check> {
        val check = translation.telegram.check
        when (val state = connectionState.value) {
            TelegramConnectionState.Disabled -> return listOf(errorCheck(check.tokenMissing))
            is TelegramConnectionState.Failed -> if (state.failure.needsNewSettings) {
                return listOf(errorCheck(failureTextMapper.map(state.failure)))
            }

            TelegramConnectionState.Connecting, is TelegramConnectionState.Connected -> Unit
        }
        val bot = botApi.execute(GetMe()).valueOrElse { failed -> return listOf(failed) }
        return listOf(okCheck(check.botWorks("@${bot.userName}"))) + chatChecks(bot)
    }
}
