package ru.astrainteractive.messagebridge.link.mapping

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.link.model.LinkResponse

fun LinkResponse.asMessage(translation: PluginTranslation.Link): LocalizableComponent {
    return when (this) {
        LinkResponse.AlreadyLinked -> translation.alreadyLinked
        LinkResponse.NoCode -> translation.noCodeFound
        LinkResponse.NoUsername -> translation.noUsername
        LinkResponse.UnknownError -> translation.unknownError
        is LinkResponse.Linked -> translation.success
    }
}
