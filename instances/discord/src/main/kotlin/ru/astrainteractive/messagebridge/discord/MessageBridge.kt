package ru.astrainteractive.messagebridge.discord

import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.messagebridge.discord.di.RootModule

object MessageBridge : Logger by JUtiltLogger("MessageBridgePlugin") {
    private val rootModule = RootModule()

    @JvmStatic
    fun main(args: Array<String>) {
        info { "Enabling jda..." }
        rootModule.jdaModule.jda.awaitReady()
        info { "Jda enabled!" }
        rootModule.lifecycle.onEnable()
    }
}
