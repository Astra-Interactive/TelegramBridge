package ru.astrainteractive.messagebridge.core.api.util

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import java.util.Locale

fun LocalizableComponent.toMessengerText(): String {
    return PlainTextComponentSerializer.plainText().serialize(toComponent(Locale.ROOT))
}
