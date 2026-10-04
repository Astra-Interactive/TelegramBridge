package ru.astrainteractive.messagebridge.link.usecase

import ru.astrainteractive.messagebridge.link.code.api.CodeApi
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.player.model.MessengerAccount

internal class LinkAccountUseCase(
    private val codeApi: CodeApi,
    private val linkingDao: LinkingDao,
    private val luckPermsRoleController: LuckPermsRoleController
) {
    private fun LinkedPlayerModel.hasAccountOf(account: MessengerAccount): Boolean = when (account) {
        is MessengerAccount.Discord -> discordLink != null
        is MessengerAccount.Telegram -> telegramLink != null
    }

    private fun LinkedPlayerModel.withAccount(account: MessengerAccount): LinkedPlayerModel = when (account) {
        is MessengerAccount.Discord -> copy(
            discordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = account.name, discordId = account.id)
        )

        is MessengerAccount.Telegram -> copy(
            telegramLink = LinkedPlayerModel.TelegramLink(telegramUsername = account.username, telegramId = account.id)
        )
    }

    suspend fun link(code: Int, account: MessengerAccount): LinkResponse {
        val codeUser = codeApi.findUserByCode(code) ?: return LinkResponse.NoCode
        codeApi.clearCode(code)
        val player = linkingDao.findByUuid(codeUser.uuid)
            .getOrElse { _ -> return LinkResponse.UnknownError }
            ?: LinkedPlayerModel(codeUser.uuid, codeUser.name)
        if (player.hasAccountOf(account)) return LinkResponse.AlreadyLinked
        return linkingDao.upsert(player.withAccount(account)).fold(
            onSuccess = { linked ->
                luckPermsRoleController.addLinkRole(linked.uuid)
                LinkResponse.Linked
            },
            onFailure = { _ -> LinkResponse.UnknownError }
        )
    }
}
