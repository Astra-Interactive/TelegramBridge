package ru.astrainteractive.messagebridge.link.mapping

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.api.LinkApi

internal fun LinkApi.Response.asMessage(translation: PluginTranslation.Link): LocalizableComponent {
    return when (this) {
        LinkApi.Response.AlreadyLinked -> translation.alreadyLinked
        LinkApi.Response.NoCode -> translation.noCodeFound
        LinkApi.Response.NoUsername -> translation.noUsername
        LinkApi.Response.UnknownError -> translation.unknownError
        is LinkApi.Response.Linked -> translation.success
    }
}
