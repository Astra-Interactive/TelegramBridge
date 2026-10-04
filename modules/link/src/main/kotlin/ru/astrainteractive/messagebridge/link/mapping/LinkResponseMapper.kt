package ru.astrainteractive.messagebridge.link.mapping

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.model.LinkResponse

internal fun LinkResponse.asMessage(translation: PluginTranslation.Link): LocalizableComponent {
    return when (this) {
        LinkResponse.Linked -> translation.success
        LinkResponse.AlreadyLinked -> translation.alreadyLinked
        LinkResponse.NoCode -> translation.noCodeFound
        LinkResponse.UnknownError -> translation.unknownError
    }
}
