package ru.astrainteractive.messagebridge.messenger.telegram.api.api

import org.telegram.telegrambots.meta.api.objects.Update

fun interface TelegramUpdateInterceptor {
    fun intercept(update: Update): Boolean
}
