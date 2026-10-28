@file:Suppress("ArgumentPerLine")

package ru.astrainteractive.messagebridge.link.usecase

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.api.LinkRoleRepository
import ru.astrainteractive.messagebridge.link.api.LinkedPlayerRepository
import ru.astrainteractive.messagebridge.link.code.api.LinkCodeRepository
import ru.astrainteractive.messagebridge.link.model.LinkOutcome
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.link.model.MessengerAccount

internal class LinkAccountUseCase(
    private val linkCodeRepository: LinkCodeRepository,
    private val linkedPlayerRepository: LinkedPlayerRepository,
    private val linkRoleRepository: LinkRoleRepository
) : Logger by JUtiltLogger("MessageBridge-LinkAccountUseCase") {
    private fun reportFailure(t: Throwable, account: MessengerAccount): LinkResponse {
        error(t) { "#invoke could not link $account" }
        return LinkResponse.UnknownError
    }

    suspend operator fun invoke(code: Int, account: MessengerAccount): LinkResponse {
        val codeUser = linkCodeRepository.findUserByCode(code) ?: return LinkResponse.NoCode
        val outcome = linkedPlayerRepository
            .link(uuid = codeUser.uuid, minecraftName = codeUser.name, account = account)
            .getOrElse { t -> return reportFailure(t, account) }
        return when (outcome) {
            LinkOutcome.Linked -> {
                linkCodeRepository.clearCode(code)
                linkRoleRepository.addLinkRole(codeUser.uuid)
                LinkResponse.Linked
            }

            LinkOutcome.AlreadyLinked -> LinkResponse.AlreadyLinked
            LinkOutcome.AccountTaken -> LinkResponse.AccountTaken
        }
    }
}
