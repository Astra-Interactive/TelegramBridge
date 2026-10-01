package ru.astrainteractive.messagebridge.messenger.telegram.onboarding

import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.MutableKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.config.describeConfigError
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.request.TelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.request.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.request.TelegramRequestResult
import ru.astrainteractive.messagebridge.onboarding.bind.BindCodes

/** Binds the chat, and the topic, a code of [BindCodes] is sent to with `/bind <code>`. */
internal class TelegramBindHandler(
    private val bindCodes: BindCodes,
    private val botApi: TelegramBotApi,
    private val messageSender: TelegramMessageSender,
    private val configKrate: MutableKrate<Result<PluginConfiguration>>,
    translationKrate: CachedKrate<PluginTranslation>,
    logger: Logger,
) : Logger by logger {
    private val translation by translationKrate

    private suspend fun reply(message: Message, text: String) {
        messageSender.send(message.chatId.toString(), text, replyToMessageId = message.messageId)
    }

    private suspend fun isAdmin(message: Message, userId: Long): Boolean {
        val chatId = message.chatId.toString()
        return when (val result = botApi.execute(GetChatMember(chatId, userId))) {
            is TelegramRequestResult.Success -> {
                val member = result.value
                member is ChatMemberAdministrator || member is ChatMemberOwner
            }

            TelegramRequestResult.NotConnected -> false
            is TelegramRequestResult.Failed -> {
                error { "#isAdmin could not get the member $userId of chat $chatId: ${result.failure}" }
                false
            }
        }
    }

    /** In a group only its admins can bind it; an anonymous admin writes on behalf of the group itself. */
    private suspend fun isAllowedToBind(message: Message): Boolean {
        val chat = message.chat
        if (chat.isGroupChat != true && chat.isSuperGroupChat != true) return true
        if (message.senderChat?.id == chat.id) return true
        val userId = message.from?.id ?: return false
        return isAdmin(message, userId)
    }

    private fun save(chatId: String, topicId: String): Result<PluginConfiguration> {
        return configKrate.saveAndGet { result ->
            result.map { configuration ->
                configuration.copy(tgConfig = configuration.tgConfig.copy(chatID = chatId, topicID = topicId))
            }
        }
    }

    private fun titleOf(message: Message): String {
        return message.chat.title ?: message.chat.userName ?: message.chatId.toString()
    }

    /** @return name of the topic when the code replies to the message that created it, its id otherwise */
    private fun topicNameOf(message: Message): String? {
        val topicId = message.topicIdOrNull() ?: return null
        return message.replyToMessage?.forumTopicCreated?.name ?: topicId
    }

    suspend fun bind(code: String, message: Message) {
        val bindTranslation = translation.telegram.bind
        if (!bindCodes.isValid(code)) {
            reply(message, bindTranslation.invalidCode.toMessengerText())
            return
        }
        if (!isAllowedToBind(message)) {
            reply(message, bindTranslation.adminsOnly.toMessengerText())
            return
        }
        val onBound = bindCodes.consume(code)
        if (onBound == null) {
            reply(message, bindTranslation.invalidCode.toMessengerText())
            return
        }
        val chatId = message.chatId.toString()
        val topicId = message.topicIdOrNull().orEmpty()
        save(chatId, topicId).fold(
            onSuccess = { _ ->
                info { "#bind chat_id is $chatId, topic_id is '$topicId'" }
                reply(message, bindTranslation.success.toMessengerText())
                onBound.invoke(bindTranslation.bound(chat = titleOf(message), topic = topicNameOf(message)))
            },
            onFailure = { throwable ->
                reply(message, bindTranslation.saveFailed.toMessengerText())
                val error = describeConfigError(throwable)
                onBound.invoke(bindTranslation.notBound(chat = titleOf(message), error = error))
            }
        )
    }
}
