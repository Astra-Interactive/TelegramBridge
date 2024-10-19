package ru.astrainteractive.messagebridge.messenger.discord.event.core

import net.dv8tion.jda.api.JDA

internal interface DiscordEventListener {
    fun onEnable(jda: JDA) {
        jda.addEventListener(this)
    }

    fun onDisable(jda: JDA) {
        jda.removeEventListener(this)
    }
}
