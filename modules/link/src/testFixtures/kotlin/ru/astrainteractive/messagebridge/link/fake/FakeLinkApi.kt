package ru.astrainteractive.messagebridge.link.fake

import net.dv8tion.jda.api.entities.Member
import org.telegram.telegrambots.meta.api.objects.User
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import java.util.concurrent.CopyOnWriteArrayList

class FakeLinkApi(
    private val response: LinkResponse
) : LinkApi {
    val telegramCodes: MutableList<Int> = CopyOnWriteArrayList()
    val telegramUsers: MutableList<User> = CopyOnWriteArrayList()

    override suspend fun linkDiscord(code: Int, member: Member): LinkResponse = error("Discord is not linked here")

    override suspend fun linkTelegram(code: Int, tgUser: User): LinkResponse {
        telegramCodes += code
        telegramUsers += tgUser
        return response
    }
}
