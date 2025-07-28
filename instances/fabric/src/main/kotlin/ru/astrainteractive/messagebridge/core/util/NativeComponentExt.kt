package ru.astrainteractive.messagebridge.core.util

import net.kyori.adventure.text.Component
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer

fun Component.toText(): MutableText {
    val string = KyoriComponentSerializer.Gson.serializer.serialize(this)
    return Text.Serializer.fromJson(string) ?: Text.empty()
}

fun Text.toComponent(): Component {
    val string = Text.Serializer.toJson(this)
    return KyoriComponentSerializer.Gson.serializer.deserialize(string)
}
