package ru.astrainteractive.messagebridge.commands.reload

import ru.astrainteractive.astralibs.command.api.registrar.PaperCommandRegistrarContext
import ru.astrainteractive.astralibs.command.api.util.command
import ru.astrainteractive.astralibs.command.api.util.requirePermission
import ru.astrainteractive.astralibs.command.api.util.runs
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.kyori.unwrap
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.util.getValue
import ru.astrainteractive.messagebridge.core.LifecyclePlugin
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation

internal class ReloadCommandRegistry(
    private val plugin: LifecyclePlugin,
    private val commandRegistrarContext: PaperCommandRegistrarContext,
    translationKrate: CachedKrate<PluginTranslation>,
    kyoriKrate: CachedKrate<KyoriComponentSerializer>,
) : KyoriComponentSerializer by kyoriKrate.unwrap() {
    val translation by translationKrate

    fun register() {
        command("mbreload") {
            runs { ctx ->
                ctx.requirePermission(PluginPermission.Reload)
                translation.reload.component
                    .let(KyoriComponentSerializer.Plain.serializer::serialize)
                    .run(ctx.source.sender::sendMessage)
                plugin.onReload()
                translation.reloadComplete.component
                    .let(KyoriComponentSerializer.Plain.serializer::serialize)
                    .run(ctx.source.sender::sendMessage)
            }
        }.build().run(commandRegistrarContext::registerWhenReady)
    }
}
