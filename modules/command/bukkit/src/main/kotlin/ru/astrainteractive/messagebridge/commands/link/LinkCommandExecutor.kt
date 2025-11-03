package ru.astrainteractive.messagebridge.commands.link

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.util.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.model.CodeUser
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import java.util.UUID

internal class LinkCommandExecutor(
    private val scope: CoroutineScope,
    private val codeApi: CodeApi,
    private val linkingDao: LinkingDao,
    translationKrate: CachedKrate<PluginTranslation>,
    kyoriKrate: CachedKrate<KyoriComponentSerializer>,
) {
    private val kyori by kyoriKrate
    private val translation by translationKrate

    sealed interface Intent {
        data class Link(val player: Player) : Intent
        data class UserInfo(val targetPlayerUuid: UUID, val sender: Player) : Intent
    }

    fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.Link -> {
                scope.launch {
                    val player = intent.player
                    val codeUser = CodeUser(
                        name = player.name,
                        uuid = player.uniqueId
                    )
                    val code = codeApi.generateCodeForPlayer(codeUser)
                    with(kyori) {
                        translation.link.codeCreated(code).component
                            .let(KyoriComponentSerializer.Plain.serializer::serialize)
                            .run(player::sendMessage)
                    }
                }
            }

            is Intent.UserInfo -> {
                scope.launch {
                    val user = linkingDao.findByUuid(intent.targetPlayerUuid).getOrNull()
                    if (user == null) {
                        intent.sender.sendMessage("User not found")
                    } else {
                        @Suppress("MaximumLineLength", "MaxLineLength")
                        intent.sender.sendMessage(
                            "DiscordID: ${user.discordLink?.discordId}; telegramUsername: ${user.telegramLink?.telegramUsername}; minecraftUUID: ${user.uuid}"
                        )
                    }
                }
            }
        }
    }
}
