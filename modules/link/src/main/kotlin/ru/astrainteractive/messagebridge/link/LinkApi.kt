package ru.astrainteractive.messagebridge.link

import net.dv8tion.jda.api.entities.Member
import org.telegram.telegrambots.meta.api.objects.User

interface LinkApi {
    suspend fun linkDiscord(code: Int, member: Member): LinkResponse

    suspend fun linkTelegram(code: Int, tgUser: User): LinkResponse
}
