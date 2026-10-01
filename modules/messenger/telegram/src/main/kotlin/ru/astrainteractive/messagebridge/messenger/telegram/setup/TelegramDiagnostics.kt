package ru.astrainteractive.messagebridge.messenger.telegram.setup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
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
import ru.astrainteractive.messagebridge.messaging.setup.DiagnosticCheck
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramFailureMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramConnection
import java.io.Serializable
import kotlin.coroutines.cancellation.CancellationException

/** Checks the settings step by step, the way messages travel: token, chat, rights of the bot, topic, delivery. */
internal class TelegramDiagnostics(
    configKrate: CachedKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
    private val connectionFlow: Flow<TelegramConnection>,
    private val failureMapper: TelegramFailureMapper,
    private val failureTextMapper: TelegramFailureTextMapper,
) {
    private val config by configKrate
    private val translation by translationKrate

    suspend fun diagnose(): List<DiagnosticCheck> {
        val check = translation.telegram.check
        val client = when (val connection = connectionFlow.first()) {
            TelegramConnection.Disabled -> return listOf(error(check.tokenMissing))
            is TelegramConnection.Invalid -> return listOf(error(failureTextMapper.map(connection.failure)))
            is TelegramConnection.Ready -> connection.telegramClient
        }
        val bot = client.request(GetMe()).getOrElse { throwable -> return listOf(failed(throwable)) }
        val botCheck = ok(check.botWorks("@${bot.userName}"))
        return listOf(botCheck) + chatChecks(client, bot)
    }

    private suspend fun chatChecks(client: OkHttpTelegramClient, bot: User): List<DiagnosticCheck> {
        val tgConfig = config.tgConfig
        if (tgConfig.chatID.isBlank()) return listOf(error(translation.telegram.errors.chatNotSet))
        val chat = client.request(GetChat(tgConfig.chatID)).getOrElse { throwable -> return listOf(failed(throwable)) }
        val chatCheck = ok(
            translation.telegram.check.chatFound(
                title = chat.title ?: chat.userName ?: tgConfig.chatID,
                type = chat.type.orEmpty(),
                isForum = chat.isForum == true
            )
        )
        return listOf(chatCheck) +
            memberChecks(client, chat, bot) +
            topicChecks(chat, tgConfig.topicID) +
            deliveryCheck(client, tgConfig)
    }

    private suspend fun memberChecks(
        client: OkHttpTelegramClient,
        chat: ChatFullInfo,
        bot: User
    ): List<DiagnosticCheck> {
        if (chat.isGroupChat != true && chat.isSuperGroupChat != true) return emptyList()
        val check = translation.telegram.check
        val errors = translation.telegram.errors
        val member = client.request(GetChatMember(chat.id.toString(), bot.id))
            .getOrElse { throwable -> return listOf(failed(throwable)) }
        return when (member) {
            is ChatMemberOwner -> listOf(ok(check.botAdmin))
            is ChatMemberAdministrator -> listOfNotNull(
                ok(check.botAdmin),
                warning(check.botCantDelete).takeIf { member.canDeleteMessages != true }
            )

            is ChatMemberLeft, is ChatMemberBanned -> listOf(error(errors.botNotInChat))
            is ChatMemberRestricted -> if (member.canSendMessages == false) {
                listOf(error(errors.noRights))
            } else {
                notAdminChecks(bot)
            }

            else -> notAdminChecks(bot)
        }
    }

    /** Without admin rights and with privacy mode on, the bot receives only the commands. */
    private fun notAdminChecks(bot: User): List<DiagnosticCheck> {
        val check = translation.telegram.check
        return listOfNotNull(
            warning(check.botNotAdmin),
            error(check.privacyMode).takeIf { bot.canReadAllGroupMessages != true }
        )
    }

    /** A topic id in a chat without topics is the root of a reply thread, which is a mode of its own. */
    private fun topicChecks(chat: ChatFullInfo, topicId: String): List<DiagnosticCheck> {
        val check = translation.telegram.check
        val isForum = chat.isForum == true
        return when {
            isForum && topicId.isBlank() -> listOf(warning(check.forumWithoutTopic))
            !isForum && topicId.isNotBlank() -> listOf(warning(check.replyThread(topicId)))
            else -> emptyList()
        }
    }

    private suspend fun deliveryCheck(
        client: OkHttpTelegramClient,
        tgConfig: PluginConfiguration.TelegramConfig
    ): DiagnosticCheck {
        val check = translation.telegram.check
        val sendMessage = SendMessage(tgConfig.chatID, check.testMessage.toMessengerText()).apply {
            replyToMessageId = tgConfig.topicID.toIntOrNull()
        }
        return client.request(sendMessage).fold(
            onSuccess = { ok(check.testMessageSent) },
            onFailure = ::failed
        )
    }

    private suspend fun <T : Serializable> OkHttpTelegramClient.request(method: BotApiMethod<T>): Result<T> {
        return runCatching { executeAsync(method).await() }
            .onFailure { throwable -> if (throwable is CancellationException) throw throwable }
    }

    private fun failed(throwable: Throwable): DiagnosticCheck {
        return error(failureTextMapper.map(failureMapper.map(throwable)))
    }

    private fun ok(message: LocalizableComponent) = DiagnosticCheck(DiagnosticCheck.Level.OK, message)

    private fun warning(message: LocalizableComponent) = DiagnosticCheck(DiagnosticCheck.Level.WARNING, message)

    private fun error(message: LocalizableComponent) = DiagnosticCheck(DiagnosticCheck.Level.ERROR, message)
}
