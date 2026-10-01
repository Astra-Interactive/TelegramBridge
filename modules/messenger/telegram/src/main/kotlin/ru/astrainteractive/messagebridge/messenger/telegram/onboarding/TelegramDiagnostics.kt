package ru.astrainteractive.messagebridge.messenger.telegram.onboarding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.connection.TelegramConnection
import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.request.TelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.request.TelegramRequestResult
import ru.astrainteractive.messagebridge.onboarding.check.Check
import ru.astrainteractive.messagebridge.onboarding.check.CheckLevel

/** Checks the settings step by step, the way messages travel: token, chat, rights of the bot, topic, delivery. */
internal class TelegramDiagnostics(
    private val configFlow: StateFlow<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
    private val connections: Flow<TelegramConnection>,
    private val botApi: TelegramBotApi,
    private val failureTextMapper: TelegramFailureTextMapper,
) {
    private val translation by translationKrate

    private fun okCheck(message: LocalizableComponent) = Check(CheckLevel.OK, message)

    private fun warningCheck(message: LocalizableComponent) = Check(CheckLevel.WARNING, message)

    private fun errorCheck(message: LocalizableComponent) = Check(CheckLevel.ERROR, message)

    /** @param onFailure receives the check that tells why the request did not succeed */
    private inline fun <T> TelegramRequestResult<T>.valueOrElse(onFailure: (Check) -> Nothing): T = when (this) {
        is TelegramRequestResult.Success -> value
        TelegramRequestResult.NotConnected -> onFailure(errorCheck(translation.telegram.check.reconnecting))
        is TelegramRequestResult.Failed -> onFailure(errorCheck(failureTextMapper.map(failure)))
    }

    /** Without admin rights and with privacy mode on, the bot receives only the commands. */
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
        val errors = translation.telegram.errors
        val member = botApi.execute(GetChatMember(chat.id.toString(), bot.id))
            .valueOrElse { failed -> return listOf(failed) }
        return when (member) {
            is ChatMemberOwner -> listOf(okCheck(check.botAdmin))
            is ChatMemberAdministrator -> listOfNotNull(
                okCheck(check.botAdmin),
                warningCheck(check.botCantDelete).takeIf { _ -> member.canDeleteMessages != true }
            )

            is ChatMemberLeft, is ChatMemberBanned -> listOf(errorCheck(errors.botNotInChat))
            is ChatMemberRestricted -> if (member.canSendMessages == false) {
                listOf(errorCheck(errors.noRights))
            } else {
                notAdminChecks(bot)
            }

            else -> notAdminChecks(bot)
        }
    }

    /** A topic id in a chat without topics is the root of a reply thread, which is a mode of its own. */
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
        if (tgConfig.chatID.isBlank()) return listOf(errorCheck(translation.telegram.errors.chatNotSet))
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
        when (val connection = connections.first()) {
            TelegramConnection.Disabled -> return listOf(errorCheck(check.tokenMissing))
            is TelegramConnection.Invalid -> return listOf(errorCheck(failureTextMapper.map(connection.failure)))
            is TelegramConnection.Ready -> Unit
        }
        val bot = botApi.execute(GetMe()).valueOrElse { failed -> return listOf(failed) }
        return listOf(okCheck(check.botWorks("@${bot.userName}"))) + chatChecks(bot)
    }
}
