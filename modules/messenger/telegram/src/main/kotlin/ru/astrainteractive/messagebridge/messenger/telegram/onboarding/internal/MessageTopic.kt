package ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal

import org.telegram.telegrambots.meta.api.objects.message.Message

internal fun Message.topicIdOrNull(): String? = messageThreadId?.takeIf { _ -> isTopicMessage() }?.toString()
