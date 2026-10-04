package ru.astrainteractive.messagebridge.link.internal

import net.dv8tion.jda.api.entities.Member
import org.telegram.telegrambots.meta.api.objects.User
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.code.api.CodeApi
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayerModel

internal class LinkApiImpl(
    private val linkingDao: LinkingDao,
    private val codeApi: CodeApi,
    private val discordRoleController: DiscordRoleController,
    private val luckPermsRoleController: LuckPermsRoleController
) : LinkApi {
    override suspend fun linkDiscord(code: Int, member: Member): LinkResponse {
        val codeUser = codeApi.findUserByCode(code)
        if (codeUser == null) return LinkResponse.NoCode
        codeApi.clearCode(code)
        val linkedPlayerModel = linkingDao.findByUuid(codeUser.uuid)
            .onFailure { return LinkResponse.UnknownError }
            .getOrNull()
            ?: LinkedPlayerModel(codeUser.uuid, codeUser.name)
        if (linkedPlayerModel.discordLink != null) return LinkResponse.AlreadyLinked
        val updatedUser = linkedPlayerModel.copy(
            discordLink = LinkedPlayerModel.DiscordLink(
                discordId = member.idLong,
                lastDiscordName = member.effectiveName
            )
        )

        linkingDao.upsert(updatedUser)
            .onSuccess { user ->
                discordRoleController.addLinkedRole(member)
                luckPermsRoleController.addLinkRole(user.uuid)
                return LinkResponse.Linked(user)
            }
        return LinkResponse.UnknownError
    }

    override suspend fun linkTelegram(code: Int, tgUser: User): LinkResponse {
        val username = tgUser.userName ?: return LinkResponse.NoUsername
        val codeUser = codeApi.findUserByCode(code)
        if (codeUser == null) return LinkResponse.NoCode
        codeApi.clearCode(code)
        val user = linkingDao.findByUuid(codeUser.uuid)
            .onFailure { return LinkResponse.UnknownError }
            .getOrNull()
            ?: LinkedPlayerModel(codeUser.uuid, codeUser.name)
        if (user.telegramLink != null) return LinkResponse.AlreadyLinked
        val updatedUser = user.copy(
            telegramLink = LinkedPlayerModel.TelegramLink(
                telegramId = tgUser.id,
                telegramUsername = username
            )
        )
        linkingDao.upsert(updatedUser)
            .onSuccess { user ->
                luckPermsRoleController.addLinkRole(user.uuid)
                return LinkResponse.Linked(user)
            }
        return LinkResponse.UnknownError
    }

    override suspend fun userLeaveDiscord(discordUserId: Long) {
        val user = linkingDao.findByDiscordId(discordUserId)
            .getOrNull() ?: return
        luckPermsRoleController.removeLinkedRole(user.uuid)
    }
}
