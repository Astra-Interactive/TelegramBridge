package ru.astrainteractive.messagebridge.link.api.mapping

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.link.api.config.LinkTranslation
import ru.astrainteractive.messagebridge.link.api.model.LinkResponse

fun LinkResponse.asMessage(translation: LinkTranslation.Link): LocalizableComponent {
    return when (this) {
        LinkResponse.AlreadyLinked -> translation.alreadyLinked
        LinkResponse.NoCode -> translation.noCodeFound
        LinkResponse.NoUsername -> translation.noUsername
        LinkResponse.UnknownError -> translation.unknownError
        is LinkResponse.Linked -> translation.success
    }
}
