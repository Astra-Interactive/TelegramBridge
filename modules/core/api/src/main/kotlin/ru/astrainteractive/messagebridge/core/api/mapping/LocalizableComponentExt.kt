package ru.astrainteractive.messagebridge.core.api.mapping

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import java.util.Locale

/**
 * Text for a Telegram or Discord chat. Such a chat is shared by everyone and has no client language, so it reads
 * the default language of the text; markup is dropped because the messengers cannot show it.
 */
fun LocalizableComponent.toMessengerText(): String {
    return PlainTextComponentSerializer.plainText().serialize(toComponent(Locale.ROOT))
}
