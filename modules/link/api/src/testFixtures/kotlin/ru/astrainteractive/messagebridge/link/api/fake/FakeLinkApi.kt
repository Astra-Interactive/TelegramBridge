package ru.astrainteractive.messagebridge.link.api.fake

import ru.astrainteractive.messagebridge.link.api.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.model.LinkResponse
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel
import java.util.concurrent.CopyOnWriteArrayList

class FakeLinkApi(
    private val response: LinkResponse
) : LinkApi {
    val discordCodes: MutableList<Int> = CopyOnWriteArrayList()
    val discordLinks: MutableList<LinkedPlayerModel.DiscordLink> = CopyOnWriteArrayList()
    val telegramCodes: MutableList<Int> = CopyOnWriteArrayList()
    val telegramLinks: MutableList<LinkedPlayerModel.TelegramLink> = CopyOnWriteArrayList()

    override suspend fun linkDiscord(code: Int, discordLink: LinkedPlayerModel.DiscordLink): LinkResponse {
        discordCodes += code
        discordLinks += discordLink
        return response
    }

    override suspend fun linkTelegram(code: Int, telegramLink: LinkedPlayerModel.TelegramLink): LinkResponse {
        telegramCodes += code
        telegramLinks += telegramLink
        return response
    }
}
