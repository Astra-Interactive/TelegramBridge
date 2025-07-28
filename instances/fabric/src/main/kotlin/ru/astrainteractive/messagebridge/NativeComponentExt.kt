package ru.astrainteractive.messagebridge

import net.kyori.adventure.text.Component
import net.minecraft.text.MutableText
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer


fun Component.toText(): MutableText {
    val string = KyoriComponentSerializer.Gson.serializer.serialize(this)
    return net.minecraft.text.Text.Serializer.fromJson(string) ?: net.minecraft.text.Text.empty()
}

fun net.minecraft.text.Text.toComponent(): Component {
    val string = net.minecraft.text.Text.Serializer.toJson(this)
    return KyoriComponentSerializer.Gson.serializer.deserialize(string)
}