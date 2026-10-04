package ru.astrainteractive.messagebridge.link.usecase

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.code.api.CodeApi
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.player.model.MessengerAccount

internal class LinkAccountUseCase(
    private val codeApi: CodeApi,
    private val linkingDao: LinkingDao,
    private val luckPermsRoleController: LuckPermsRoleController
) : Logger by JUtiltLogger("MessageBridge-LinkAccountUseCase") {
    private fun LinkedPlayer.hasAccountOf(account: MessengerAccount): Boolean = when (account) {
        is MessengerAccount.Discord -> discord != null
        is MessengerAccount.Telegram -> telegram != null
    }

    private suspend fun findOwner(account: MessengerAccount): Result<LinkedPlayer?> = when (account) {
        is MessengerAccount.Discord -> linkingDao.findByDiscordId(account.id)
        is MessengerAccount.Telegram -> linkingDao.findByTelegramId(account.id)
    }

    private fun reportFailure(t: Throwable, account: MessengerAccount): LinkResponse {
        error(t) { "#link could not link $account" }
        return LinkResponse.UnknownError
    }

    suspend fun link(code: Int, account: MessengerAccount): LinkResponse {
        val codeUser = codeApi.findUserByCode(code) ?: return LinkResponse.NoCode
        val player = linkingDao.findByUuid(codeUser.uuid).getOrElse { t -> return reportFailure(t, account) }
        if (player?.hasAccountOf(account) == true) return LinkResponse.AlreadyLinked
        val owner = findOwner(account).getOrElse { t -> return reportFailure(t, account) }
        if (owner != null) return LinkResponse.AccountTaken
        return linkingDao.link(uuid = codeUser.uuid, minecraftName = codeUser.name, account = account).fold(
            onSuccess = { _ ->
                codeApi.clearCode(code)
                luckPermsRoleController.addLinkRole(codeUser.uuid)
                LinkResponse.Linked
            },
            onFailure = { t -> reportFailure(t, account) }
        )
    }
}
