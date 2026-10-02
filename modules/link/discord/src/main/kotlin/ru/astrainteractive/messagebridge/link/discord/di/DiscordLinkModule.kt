package ru.astrainteractive.messagebridge.link.discord.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.di.LinkModule
import ru.astrainteractive.messagebridge.link.api.di.LinkTranslationModule
import ru.astrainteractive.messagebridge.link.discord.command.DiscordLinkHandler
import ru.astrainteractive.messagebridge.link.discord.event.DiscordLinkInterceptor
import ru.astrainteractive.messagebridge.link.discord.event.DiscordMemberLeaveListener
import ru.astrainteractive.messagebridge.link.discord.internal.DiscordLinkRole
import ru.astrainteractive.messagebridge.link.discord.internal.DiscordMemberSweep
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordMessageInterceptor
import ru.astrainteractive.messagebridge.messenger.discord.api.di.DiscordBotModule
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordConnection

class DiscordLinkModule(
    coreModule: CoreModule,
    linkModule: LinkModule,
    linkTranslationModule: LinkTranslationModule,
    botModule: DiscordBotModule,
) {
    private val moduleIoScope = coreModule.ioScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(coreModule.ioScope.coroutineContext::plus)
        .let(::CoroutineScope)

    private val memberLeaveListener = DiscordMemberLeaveListener(
        configFlow = coreModule.config,
        discordMembership = linkModule.discordMembership,
        scope = moduleIoScope,
    )

    private val memberSweep = DiscordMemberSweep(
        configFlow = coreModule.config,
        discordMembership = linkModule.discordMembership,
    )

    val messageInterceptor: DiscordMessageInterceptor = DiscordLinkInterceptor(
        scope = moduleIoScope,
        configFlow = coreModule.config,
        linkHandler = DiscordLinkHandler(
            linkApi = linkModule.linkApi,
            linkRole = DiscordLinkRole(),
            messageSender = botModule.messageSender,
            configFlow = coreModule.config,
            translationKrate = linkTranslationModule.translationKrate,
        ),
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            botModule.connection
                .map { connection -> connection.tryCast<DiscordConnection.Connected>()?.jda }
                .filterNotNull()
                .distinctUntilChanged()
                .onEach { jda ->
                    jda.addEventListener(memberLeaveListener)
                    memberSweep.sweep(jda)
                }
                .launchIn(moduleIoScope)
        },
        onDisable = {
            moduleIoScope.cancel()
            botModule.connection.value.tryCast<DiscordConnection.Connected>()
                ?.jda
                ?.removeEventListener(memberLeaveListener)
        }
    )
}
