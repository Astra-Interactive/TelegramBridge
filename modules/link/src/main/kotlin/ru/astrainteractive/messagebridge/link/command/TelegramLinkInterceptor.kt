package ru.astrainteractive.messagebridge.link.command

import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.dao.model.MessengerAccount
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.link.usecase.LinkAccountUseCase
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messaging.model.Interception

internal class TelegramLinkInterceptor(
    private val linkAccountUseCase: LinkAccountUseCase,
    translationKrate: CachedKrate<PluginTranslation>
) : MessageInterceptor<Update> {
    private val translation by translationKrate

    override suspend fun intercept(event: Update): Interception {
        val message = event.message ?: return Interception.Pass
        val code = message.text?.toLinkCode() ?: return Interception.Pass
        val user = message.from ?: return Interception.Consumed
        val username = user.userName ?: return Interception.Reply(translation.link.noUsername.toMessengerText())
        val response = linkAccountUseCase.link(code, MessengerAccount.Telegram(id = user.id, username = username))
        return Interception.Reply(response.asMessage(translation.link).toMessengerText())
    }
}
