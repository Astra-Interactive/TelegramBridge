package ru.astrainteractive.messagebridge.messenger.forge.util

import net.kyori.adventure.text.Component
import net.minecraft.network.chat.Component.Serializer
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer

fun Component.toNative(): net.minecraft.network.chat.Component {
    val json = KyoriComponentSerializer.Json
    val jsonComponent = json.serializer.serialize(this)
    return Serializer.fromJson(jsonComponent) ?: net.minecraft.network.chat.Component.empty()
}
