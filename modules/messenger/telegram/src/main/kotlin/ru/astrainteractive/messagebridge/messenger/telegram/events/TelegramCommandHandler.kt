package ru.astrainteractive.messagebridge.messenger.telegram.events

import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.CachedMutableKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.messaging.setup.BindCodes
import ru.astrainteractive.messagebridge.messenger.telegram.messaging.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramCommand

internal class TelegramCommandHandler(
    private val messageSender: TelegramMessageSender,
    private val onlinePlayersProvider: OnlinePlayersProvider,
    private val linkApi: LinkApi,
    private val bindCodes: BindCodes,
    private val configKrate: CachedMutableKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-TelegramCommandHandler") {
    private val translation by translationKrate

    suspend fun handle(command: TelegramCommand, update: Update) {
        val message = update.message ?: return
        val chatId = message.chatId.toString()
        val originalMessageId = message.replyToMessage?.messageId
        when (command) {
            TelegramCommand.Vanilla -> sendVanilla(chatId, originalMessageId)
            is TelegramCommand.Link -> sendLink(command.code, message.from, chatId, originalMessageId)
            is TelegramCommand.Bind -> bind(command.code, message)
            TelegramCommand.ChatInfo -> sendChatInfo(message)
        }
    }

    private suspend fun sendVanilla(chatId: String, originalMessageId: Int?) {
        val players = onlinePlayersProvider.provide()
        val text = translation.onlinePlayers.message(
            count = players.size,
            players = players.joinToString(separator = ", "),
        ).toMessengerText()
        messageSender.send(chatId, text, originalMessageId)
    }

    private suspend fun sendLink(code: Int, user: User?, chatId: String, originalMessageId: Int?) {
        user ?: return
        val response = linkApi.linkTelegram(code, user)
        val text = response.asMessage(translation.link).toMessengerText()
        messageSender.send(chatId, text, originalMessageId)
    }

    private suspend fun sendChatInfo(message: Message) {
        val chatId = message.chatId.toString()
        val topicId = message.topicIdOrNull() ?: NO_TOPIC
        val isForum = message.chat.isForum == true
        info {
            "#sendChatInfo chat_id: $chatId; topic_id: $topicId; type: ${message.chat.type}; forum: $isForum; " +
                "replied message: ${message.replyToMessage?.messageId}"
        }
        val text = translation.telegram.chatInfo.message(
            chatId = chatId,
            topicId = topicId,
            type = message.chat.type.orEmpty(),
            isForum = isForum
        )
        reply(message, text.toMessengerText())
    }

    private suspend fun bind(code: String, message: Message) {
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
        configKrate.save { configuration ->
            configuration.copy(tgConfig = configuration.tgConfig.copy(chatID = chatId, topicID = topicId))
        }
        info { "#bind chat_id is $chatId, topic_id is '$topicId'" }
        reply(message, bindTranslation.success.toMessengerText())
        val chatTitle = message.chat.title ?: message.chat.userName ?: chatId
        val topic = message.topicIdOrNull()?.let { id -> message.replyToMessage?.forumTopicCreated?.name ?: id }
        onBound.invoke(bindTranslation.bound(chat = chatTitle, topic = topic))
    }

    /** In a group only its admins can bind it; an anonymous admin writes on behalf of the group itself. */
    private suspend fun isAllowedToBind(message: Message): Boolean {
        val chat = message.chat
        if (chat.isGroupChat != true && chat.isSuperGroupChat != true) return true
        if (message.senderChat?.id == chat.id) return true
        val userId = message.from?.id ?: return false
        val member = runCatching { messageSender.execute(GetChatMember(chat.id.toString(), userId)) }
            .onFailure { error(it) { "#isAllowedToBind could not get the member $userId: ${it.message}" } }
            .getOrNull()
        return member?.status in ADMIN_STATUSES
    }

    private suspend fun reply(message: Message, text: String) {
        messageSender.send(message.chatId.toString(), text, replyToMessageId = message.messageId)
    }

    private fun Message.topicIdOrNull(): String? = messageThreadId?.takeIf { isTopicMessage() }?.toString()

    private companion object {
        const val NO_TOPIC = "none"
        val ADMIN_STATUSES = setOf("administrator", "creator")
    }
}
