package ru.astrainteractive.messagebridge.util

import net.kyori.adventure.text.Component
import net.minecraft.network.chat.Component.Serializer
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer

object NativeComponentExt {
    fun Component.toNative(): net.minecraft.network.chat.Component {
        val json = KyoriComponentSerializer.Json
        val jsonComponent = json.serializer.serialize(this)
        return Serializer.fromJson(jsonComponent) ?: net.minecraft.network.chat.Component.empty()
    }
}
