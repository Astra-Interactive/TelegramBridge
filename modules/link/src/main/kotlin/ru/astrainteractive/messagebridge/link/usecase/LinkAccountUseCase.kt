package ru.astrainteractive.messagebridge.link.usecase

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.code.api.CodeApi
import ru.astrainteractive.messagebridge.link.dao.api.LinkingDao
import ru.astrainteractive.messagebridge.link.dao.model.LinkOutcome
import ru.astrainteractive.messagebridge.link.dao.model.MessengerAccount
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.model.LinkResponse

internal class LinkAccountUseCase(
    private val codeApi: CodeApi,
    private val linkingDao: LinkingDao,
    private val luckPermsRoleController: LuckPermsRoleController
) : Logger by JUtiltLogger("MessageBridge-LinkAccountUseCase") {
    private fun reportFailure(t: Throwable, account: MessengerAccount): LinkResponse {
        error(t) { "#link could not link $account" }
        return LinkResponse.UnknownError
    }

    suspend fun link(code: Int, account: MessengerAccount): LinkResponse {
        val codeUser = codeApi.findUserByCode(code) ?: return LinkResponse.NoCode
        val outcome = linkingDao.link(uuid = codeUser.uuid, minecraftName = codeUser.name, account = account)
            .getOrElse { t -> return reportFailure(t, account) }
        return when (outcome) {
            LinkOutcome.Linked -> {
                codeApi.clearCode(code)
                luckPermsRoleController.addLinkRole(codeUser.uuid)
                LinkResponse.Linked
            }

            LinkOutcome.AlreadyLinked -> LinkResponse.AlreadyLinked
            LinkOutcome.AccountTaken -> LinkResponse.AccountTaken
        }
    }
}
