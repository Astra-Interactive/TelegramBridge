package ru.astrainteractive.messagebridge.link.dao.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID

internal object PlayerTable : Table("link_player") {
    val uuid = javaUUID("uuid")
    val minecraftName = text("minecraft_name")

    override val primaryKey = PrimaryKey(uuid)
}
