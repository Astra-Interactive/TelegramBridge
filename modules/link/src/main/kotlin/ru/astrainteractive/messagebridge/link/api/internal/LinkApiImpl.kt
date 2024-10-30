package ru.astrainteractive.messagebridge.link.api.internal

import net.dv8tion.jda.api.entities.Member
import org.telegram.telegrambots.meta.api.objects.User
import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.LinkApi.Response
import ru.astrainteractive.messagebridge.link.controller.DiscordRoleController
import ru.astrainteractive.messagebridge.link.controller.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.link.database.model.LinkedPlayerModel

class LinkApiImpl(
    private val linkingDao: LinkingDao,
    private val codeApi: CodeApi,
    private val discordRoleController: DiscordRoleController,
    private val luckPermsRoleController: LuckPermsRoleController
) : LinkApi {
    override suspend fun linkDiscord(code: Int, member: Member): Response {
        val codeUser = codeApi.getUser(code)
        if (codeUser == null) return Response.NoCode
        codeApi.clearCode(code)
        val user = linkingDao.findByUuid(codeUser.uuid)
            .onFailure { return Response.UnknownError }
            .getOrNull()
            ?: LinkedPlayerModel(codeUser.uuid, codeUser.name)
        if (user.discordLink != null) return Response.AlreadyLinked
        val updatedUser = user.copy(
            discordLink = LinkedPlayerModel.DiscordLink(
                discordId = member.idLong,
                lastDiscordName = member.effectiveName
            )
        )

        linkingDao.upsert(updatedUser)
            .onSuccess { user ->
                discordRoleController.addLinkedRole(member)
                luckPermsRoleController.addLinkRole(user.uuid)
                return Response.Linked(user)
            }
        return Response.UnknownError
    }

    override suspend fun linkTelegram(code: Int, tgUser: User): Response {
        val codeUser = codeApi.getUser(code)
        if (codeUser == null) return Response.NoCode
        codeApi.clearCode(code)
        val user = linkingDao.findByUuid(codeUser.uuid)
            .onFailure { return Response.UnknownError }
            .getOrNull()
            ?: LinkedPlayerModel(codeUser.uuid, codeUser.name)
        if (user.telegramLink != null) return Response.AlreadyLinked
        val updatedUser = user.copy(
            telegramLink = LinkedPlayerModel.TelegramLink(
                telegramId = tgUser.id,
                telegramUsername = tgUser.userName
            )
        )
        linkingDao.upsert(updatedUser)
            .onSuccess { user ->
                luckPermsRoleController.addLinkRole(user.uuid)
                return Response.Linked(user)
            }
        return Response.UnknownError
    }

    override suspend fun userLeaveDiscord(discordUser: net.dv8tion.jda.api.entities.User) {
        val user = linkingDao.findByDiscordId(discordUser.idLong)
            .getOrNull() ?: return
        luckPermsRoleController.removeLinkedRole(user.uuid)
    }
}
