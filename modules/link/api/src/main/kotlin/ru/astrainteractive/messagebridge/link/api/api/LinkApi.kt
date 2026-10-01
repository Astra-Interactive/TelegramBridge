package ru.astrainteractive.messagebridge.link.api.api

import ru.astrainteractive.messagebridge.link.api.model.LinkResponse
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel

interface LinkApi {
    suspend fun linkDiscord(code: Int, discordLink: LinkedPlayerModel.DiscordLink): LinkResponse

    suspend fun linkTelegram(code: Int, telegramLink: LinkedPlayerModel.TelegramLink): LinkResponse
}
