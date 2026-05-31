package ru.astrainteractive.messagebridge.messenger.discord.di

import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import okhttp3.Credentials
import okhttp3.OkHttpClient
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.event.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.event.MessageEventListener
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordEmbedMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordWebhookMessageMapper
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordBEventConsumer
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMemberResolver
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordTopicUpdater
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.time.Duration.Companion.seconds

class JdaMessengerModule(
    coreModule: CoreModule,
    linkModule: LinkModule,
    onlinePlayersProvider: OnlinePlayersProvider
) : Logger by JUtiltLogger("MessageBridge-JdaMessengerModule").withoutParentHandlers() {

    private val jdaFlow = coreModule.configKrate.cachedStateFlow
        .map { pluginConfiguration -> pluginConfiguration.jdaConfig }
        .flatMapLatest { config ->
            callbackFlow {
                val builder = JDABuilder.createLight(config.token).apply {
                    enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    enableIntents(GatewayIntent.DIRECT_MESSAGES)
                    enableIntents(GatewayIntent.GUILD_MESSAGES)
                    setActivity(Activity.playing(config.activity))
                    config.proxy?.let { proxy ->
                        val credential: String = Credentials.basic(proxy.username, proxy.password)
                        setWebsocketFactory(
                            WebSocketFactory()
                                .setVerifyHostname(false)
                                .also {
                                    it.proxySettings.setHost(proxy.host)
                                    it.proxySettings.setPort(proxy.port)
                                    it.proxySettings.setCredentials(proxy.username, proxy.password)
                                }
                        )
                        setHttpClientBuilder(
                            OkHttpClient.Builder()
                                .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)))
                                .proxyAuthenticator { route, response ->
                                    var builder = response.request.newBuilder()
                                    if (route?.socketAddress?.hostString == proxy.host) {
                                        builder = builder.header("Proxy-Authorization", credential)
                                    }
                                    builder.build()
                                }
                        )
                    }
                }

                val jda = flow { emit(builder.build().awaitReady()) }
                    .retryWhen { t, _ ->
                        error { "#jdaFlow could not create JDA: ${t.localizedMessage}" }
                        delay(5.seconds)
                        true
                    }
                    .first()

                send(jda)

                awaitClose {
                    jda.shutdownNow()
                    jda.awaitShutdown()
                    jda.registeredListeners.forEach(jda::removeEventListener)
                }
            }
        }

    private val webhookClient = combine(
        flow = jdaFlow,
        flow2 = coreModule.configKrate.cachedStateFlow.map { it.jdaConfig.channelId },
        transform = { jda, channelId ->
            callbackFlow {
                val webhookClient = WebHookClientFactory(jda).create(channelId).first()
                send(webhookClient)
                awaitClose {
                    webhookClient.close()
                }
            }
        }
    ).flattenConcat().shareIn(coreModule.ioScope, SharingStarted.Eagerly, 1)

    private val channelProvider = DiscordChannelProvider(
        jdaFlow = jdaFlow,
        webHookClientFlow = webhookClient,
        configKrate = coreModule.configKrate,
    )

    private val discordMessageController = DiscordBEventConsumer(
        channelProvider = channelProvider,
        topicUpdater = DiscordTopicUpdater(onlinePlayersProvider),
        embedMapper = DiscordEmbedMapper(),
        memberResolver = DiscordMemberResolver(linkModule.linkingDao),
        webhookMessageMapper = DiscordWebhookMessageMapper(),
    )

    private val relevanceMapper = DiscordMessageRelevanceMapper(
        configKrate = coreModule.configKrate,
    )

    private val commandMapper = DiscordCommandMapper()

    private val messageSender = DiscordMessageSender()

    private val commandHandler = DiscordCommandHandler(
        messageSender = messageSender,
        onlinePlayersProvider = onlinePlayersProvider,
        linkApi = linkModule.linkApi,
        translationKrate = coreModule.translationKrate,
    )

    private val messageEventListener = MessageEventListener(
        relevanceMapper = relevanceMapper,
        commandMapper = commandMapper,
        commandHandler = commandHandler,
        linkApi = linkModule.linkApi,
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            jdaFlow
                .filterNotNull()
                .onEach { jda -> messageEventListener.onEnable(jda) }
                .launchIn(coreModule.ioScope)
        },
        onDisable = {
            discordMessageController.cancel()
            messageEventListener.cancel()
            GlobalScope.launch(coreModule.dispatchers.IO) {
                jdaFlow.firstOrNull()?.let { jda ->
                    messageEventListener.onDisable(jda)
                    jda.registeredListeners.forEach(jda::removeEventListener)
                    jda.shutdownNow()
                    jda.awaitShutdown()
                }
            }
        }
    )
}
