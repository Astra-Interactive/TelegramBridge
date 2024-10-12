package ru.astrainteractive.messagebridge.discord.di

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import okhttp3.Dns
import ru.astrainteractive.messagebridge.discord.core.di.JdaCoreModule
import java.net.InetAddress

class JdaModule(jdaCoreModule: JdaCoreModule) {
    private object CloudflareDns : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return InetAddress.getAllByName("1.1.1.1").toList()
        }
    }

    val jda: JDA = let {
        val config = jdaCoreModule.config.cachedValue
        JDABuilder.createLight(config.token).apply {
            enableIntents(GatewayIntent.MESSAGE_CONTENT)
            setActivity(Activity.playing(config.activity))
//            setHttpClientBuilder(
//                OkHttpClient.Builder()
//                    .dns(CloudflareDns)
//                    .proxy(Proxy(Proxy.Type.DIRECT, InetSocketAddress.createUnresolved("89.150.35.252", 1111)))
//            )
        }.build()
    }
}
