package ru.astrainteractive.messagebridge.discord.event.core

import net.dv8tion.jda.api.JDA

interface DiscordEventListener {
    fun onEnable(jda: JDA) {
        jda.addEventListener(this)
    }

    fun onDisable(jda: JDA) {
        jda.removeEventListener(this)
    }
}
