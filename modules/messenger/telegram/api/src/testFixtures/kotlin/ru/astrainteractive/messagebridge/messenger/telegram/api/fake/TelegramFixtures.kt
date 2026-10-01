package ru.astrainteractive.messagebridge.messenger.telegram.api.fake

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import kotlin.time.Instant

const val CHAT_ID = -1001234567890L
const val BOT_USER_NAME = "MyBridgeBot"
private const val NOW_EPOCH_SECONDS = 1_700_000_000L
val NOW: Instant = Instant.fromEpochSeconds(NOW_EPOCH_SECONDS)

fun configurationOf(
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

fun userOf(
    id: Long = 7L,
    firstName: String = "Steve",
    lastName: String? = null,
    userName: String? = null,
    isBot: Boolean = false
): User = User(id, firstName, isBot).apply {
    this.lastName = lastName
    this.userName = userName
}

fun chatOf(
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

fun messageOf(
    chat: Chat = chatOf(),
    text: String? = "hello",
    from: User? = userOf(),
    messageId: Int = 100,
    date: Instant? = NOW,
    threadId: Int? = null,
    isTopicMessage: Boolean = false,
    replyTo: Message? = null,
    senderChat: Chat? = null
): Message = Message().apply {
    this.chat = chat
    this.text = text
    this.from = from
    this.messageId = messageId
    this.date = date?.epochSeconds?.toInt()
    this.messageThreadId = threadId
    setIsTopicMessage(isTopicMessage)
    this.replyToMessage = replyTo
    this.senderChat = senderChat
}

fun updateOf(message: Message?): Update = Update().apply { this.message = message }
