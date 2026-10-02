package ru.astrainteractive.messagebridge.link.impl.player.database

import ru.astrainteractive.klibs.mikro.exposed.dao.StringIdTable

internal object MinecraftPlayerTable : StringIdTable("minecraft_player", "uuid") {
    val lastMinecraftName = text("last_minecraft_name")
}
