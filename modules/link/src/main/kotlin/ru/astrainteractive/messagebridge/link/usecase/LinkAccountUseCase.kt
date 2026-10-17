package ru.astrainteractive.messagebridge.link.usecase

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.api.LinkedPlayerRepository
import ru.astrainteractive.messagebridge.link.code.api.LinkCodeRepository
import ru.astrainteractive.messagebridge.link.model.LinkOutcome
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.link.model.MessengerAccount
import ru.astrainteractive.messagebridge.link.role.luckperms.LuckPermsRoleController

internal class LinkAccountUseCase(
    private val linkCodeRepository: LinkCodeRepository,
    private val linkedPlayerRepository: LinkedPlayerRepository,
    private val luckPermsRoleController: LuckPermsRoleController
) : Logger by JUtiltLogger("MessageBridge-LinkAccountUseCase") {
    private fun reportFailure(t: Throwable, account: MessengerAccount): LinkResponse {
        error(t) { "#link could not link $account" }
        return LinkResponse.UnknownError
    }

    suspend fun link(code: Int, account: MessengerAccount): LinkResponse {
        val codeUser = linkCodeRepository.findUserByCode(code) ?: return LinkResponse.NoCode
        val outcome = linkedPlayerRepository
            .link(uuid = codeUser.uuid, minecraftName = codeUser.name, account = account)
            .getOrElse { t -> return reportFailure(t, account) }
        return when (outcome) {
            LinkOutcome.Linked -> {
                linkCodeRepository.clearCode(code)
                luckPermsRoleController.addLinkRole(codeUser.uuid)
                LinkResponse.Linked
            }

            LinkOutcome.AlreadyLinked -> LinkResponse.AlreadyLinked
            LinkOutcome.AccountTaken -> LinkResponse.AccountTaken
        }
    }
}
