package ru.astrainteractive.messagebridge.integration.luckperms.di

import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.integration.luckperms.role.LuckPermsLinkRoleRepository
import ru.astrainteractive.messagebridge.integration.luckperms.role.api.LinkRoleRepository

class LuckPermsModule(
    coreModule: CoreModule,
    luckPermsProvider: LuckPermsProvider
) {
    val linkRoleRepository: LinkRoleRepository = LuckPermsLinkRoleRepository(
        configKrate = coreModule.configKrate,
        luckPermsProvider = luckPermsProvider
    )
}
