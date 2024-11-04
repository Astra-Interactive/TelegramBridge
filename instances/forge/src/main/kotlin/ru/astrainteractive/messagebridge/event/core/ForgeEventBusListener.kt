package ru.astrainteractive.messagebridge.event.core
import net.minecraftforge.common.MinecraftForge

interface ForgeEventBusListener {

    fun register() {
        MinecraftForge.EVENT_BUS.register(this)
    }

    fun unregister() {
        MinecraftForge.EVENT_BUS.unregister(this)
    }
}
