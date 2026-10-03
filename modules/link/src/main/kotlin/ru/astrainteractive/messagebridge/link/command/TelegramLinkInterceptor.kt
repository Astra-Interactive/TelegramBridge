package ru.astrainteractive.messagebridge.link.command

import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.config.LinkTranslation
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messaging.model.Interception

internal class TelegramLinkInterceptor(
    private val linkApi: LinkApi,
    linkTranslationKrate: CachedKrate<LinkTranslation>
) : MessageInterceptor<Update> {
    private val linkTranslation by linkTranslationKrate

    override suspend fun intercept(event: Update): Interception {
        val message = event.message ?: return Interception.Pass
        val code = message.text?.toLinkCode() ?: return Interception.Pass
        val user = message.from ?: return Interception.Consumed
        val response = linkApi.linkTelegram(code, user)
        return Interception.Reply(response.asMessage(linkTranslation.link).toMessengerText())
    }
}
