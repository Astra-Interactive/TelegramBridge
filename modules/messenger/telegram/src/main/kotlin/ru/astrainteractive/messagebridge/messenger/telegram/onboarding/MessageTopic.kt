package ru.astrainteractive.messagebridge.messenger.telegram.onboarding

import org.telegram.telegrambots.meta.api.objects.message.Message

/** @return id of the forum topic the message is in, `null` in a chat without topics and in General */
internal fun Message.topicIdOrNull(): String? = messageThreadId?.takeIf { _ -> isTopicMessage() }?.toString()
