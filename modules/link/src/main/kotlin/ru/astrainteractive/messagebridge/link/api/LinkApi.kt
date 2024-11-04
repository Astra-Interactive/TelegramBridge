package ru.astrainteractive.messagebridge.link.api

import net.dv8tion.jda.api.entities.Member
import org.telegram.telegrambots.meta.api.objects.User
import ru.astrainteractive.messagebridge.link.database.model.LinkedPlayerModel

interface LinkApi {
    sealed interface Response {
        data object AlreadyLinked : Response
        data object NoCode : Response
        data object UnknownError : Response
        data class Linked(val user: LinkedPlayerModel) : Response
    }

    suspend fun linkDiscord(code: Int, member: Member): Response

    suspend fun linkTelegram(code: Int, tgUser: User): Response

    suspend fun userLeaveDiscord(discordUser: net.dv8tion.jda.api.entities.User)
}
