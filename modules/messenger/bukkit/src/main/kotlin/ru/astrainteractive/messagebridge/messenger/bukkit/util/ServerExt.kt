package ru.astrainteractive.messagebridge.messenger.bukkit.util

object ServerExt {
    private val isPaperInternal by lazy {
        kotlin.runCatching { Class.forName("com.destroystokyo.paper.PaperConfig") }
            .getOrNull() != null
    }

    fun isPaper() = isPaperInternal
}
