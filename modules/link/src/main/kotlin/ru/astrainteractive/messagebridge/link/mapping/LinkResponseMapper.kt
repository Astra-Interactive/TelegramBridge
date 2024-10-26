package ru.astrainteractive.messagebridge.link.mapping

import ru.astrainteractive.astralibs.string.StringDesc
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.api.LinkApi

fun LinkApi.Response.asMessage(translation: PluginTranslation.Link): StringDesc.Raw {
    return when (this) {
        LinkApi.Response.AlreadyLinked -> translation.alreadyLinked
        LinkApi.Response.NoCode -> translation.noCodeFound
        LinkApi.Response.UnknownError -> translation.unknownError
        is LinkApi.Response.Linked -> translation.linkSuccess
    }
}
