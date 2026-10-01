package ru.astrainteractive.messagebridge.messenger.telegram

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.messagebridge.core.PluginConfiguration

internal const val CHAT_ID = -1001234567890L
internal const val NOW_SECONDS = 1_700_000_000
internal const val BOT_USER_NAME = "MyBridgeBot"

internal fun configurationOf(
    token: String = "123:token",
    chatId: String = "$CHAT_ID",
    topicId: String = "",
    maxMessageLength: Int = 90,
    displayNameRegex: String = ".*",
    proxy: PluginConfiguration.Proxy? = null,
    apiUrl: String = ""
): PluginConfiguration = PluginConfiguration(
    tgConfig = PluginConfiguration.TelegramConfig(
        token = token,
        chatID = chatId,
        topicID = topicId,
        maxTelegramMessageLength = maxMessageLength,
        displayNameRegex = displayNameRegex,
        proxy = proxy,
        apiUrl = apiUrl
    )
)

internal fun userOf(
    id: Long = 7L,
    firstName: String = "Steve",
    lastName: String? = null,
    userName: String? = null,
    isBot: Boolean = false
): User = User(id, firstName, isBot).apply {
    this.lastName = lastName
    this.userName = userName
}

internal fun chatOf(
    id: Long = CHAT_ID,
    type: String = "supergroup",
    title: String? = "Server chat",
    userName: String? = null,
    isForum: Boolean? = null
): Chat = Chat(id, type).apply {
    this.title = title
    this.userName = userName
    this.isForum = isForum
}

internal fun messageOf(
    chat: Chat = chatOf(),
    text: String? = "hello",
    from: User? = userOf(),
    messageId: Int = 100,
    date: Int? = NOW_SECONDS,
    threadId: Int? = null,
    isTopicMessage: Boolean = false,
    replyTo: Message? = null,
    senderChat: Chat? = null
): Message = Message().apply {
    this.chat = chat
    this.text = text
    this.from = from
    this.messageId = messageId
    this.date = date
    this.messageThreadId = threadId
    setIsTopicMessage(isTopicMessage)
    this.replyToMessage = replyTo
    this.senderChat = senderChat
}

internal fun updateOf(message: Message?): Update = Update().apply { this.message = message }
