package ru.astrainteractive.messagebridge.link.impl.internal

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.link.api.api.DiscordMembership
import ru.astrainteractive.messagebridge.link.api.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.model.LinkResponse
import ru.astrainteractive.messagebridge.link.api.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.impl.api.Unlinking
import ru.astrainteractive.messagebridge.link.impl.code.api.CodeApi
import ru.astrainteractive.messagebridge.link.impl.code.model.CodeUser
import ru.astrainteractive.messagebridge.link.impl.model.UnlinkResponse
import ru.astrainteractive.messagebridge.link.impl.player.api.DiscordLinkedPlayerDao
import ru.astrainteractive.messagebridge.link.impl.role.api.PermissionGroups
import java.util.UUID

internal class LinkApiImpl(
    private val linkingDao: LinkingDao,
    private val discordLinkedPlayerDao: DiscordLinkedPlayerDao,
    private val codeApi: CodeApi,
    private val permissionGroups: PermissionGroups,
    private val configFlow: StateFlow<PluginConfiguration>,
) : LinkApi,
    DiscordMembership,
    Unlinking,
    Logger by JUtiltLogger("MessageBridge-LinkApi") {
    private val config: PluginConfiguration
        get() = configFlow.value

    private val linkMutex = Mutex()

    private suspend fun redeem(code: Int): CodeUser? {
        val codeUser = codeApi.findUserByCode(code) ?: return null
        codeApi.clearCode(code)
        return codeUser
    }

    private suspend fun findOrCreate(codeUser: CodeUser): Result<LinkedPlayerModel> {
        return linkingDao.findByUuid(codeUser.uuid).map { linkedPlayer ->
            linkedPlayer ?: LinkedPlayerModel(
                uuid = codeUser.uuid,
                lastMinecraftName = codeUser.name,
                discordLink = null,
                telegramLink = null
            )
        }
    }

    private fun unknownError(action: String, failure: Throwable): LinkResponse {
        error(failure) { "#$action could not update the link" }
        return LinkResponse.UnknownError
    }

    private suspend fun giveGroup(uuid: UUID) {
        val link = config.link ?: return
        permissionGroups.add(uuid, link.linkLuckPermsRole)
            .onFailure { t -> error(t) { "#giveGroup could not give ${link.linkLuckPermsRole} to $uuid" } }
    }

    private suspend fun unlinkLeftDiscord(linkedPlayer: LinkedPlayerModel, link: PluginConfiguration.Link) {
        val uuid = linkedPlayer.uuid
        if (linkedPlayer.telegramLink == null) {
            permissionGroups.remove(uuid, link.linkLuckPermsRole).onFailure { t ->
                error(t) { "#unlinkLeftDiscord could not take ${link.linkLuckPermsRole} from $uuid" }
                return
            }
        }
        val unlinkedPlayer = linkedPlayer.copy(discordLink = null)
        val saved = if (unlinkedPlayer.telegramLink == null) {
            linkingDao.deleteByUuid(uuid)
        } else {
            linkingDao.upsert(unlinkedPlayer).map { _ -> }
        }
        saved
            .onSuccess { _ -> info { "#unlinkLeftDiscord $uuid left the Discord server and is unlinked from it" } }
            .onFailure { t -> error(t) { "#unlinkLeftDiscord could not unlink Discord of $uuid" } }
    }

    private fun unlinkFailed(uuid: UUID, failure: Throwable): UnlinkResponse {
        error(failure) { "#unlink could not unlink $uuid" }
        return UnlinkResponse.UnknownError
    }

    override suspend fun linkDiscord(code: Int, discordLink: LinkedPlayerModel.DiscordLink): LinkResponse {
        return linkMutex.withLock {
            val codeUser = redeem(code) ?: return LinkResponse.NoCode
            val linkedPlayer = findOrCreate(codeUser)
                .getOrElse { t -> return unknownError("linkDiscord", t) }
            if (linkedPlayer.discordLink != null) return LinkResponse.AlreadyLinked
            val savedPlayer = linkingDao.upsert(linkedPlayer.copy(discordLink = discordLink))
                .getOrElse { t -> return unknownError("linkDiscord", t) }
            giveGroup(savedPlayer.uuid)
            LinkResponse.Linked(savedPlayer)
        }
    }

    override suspend fun linkTelegram(code: Int, telegramLink: LinkedPlayerModel.TelegramLink): LinkResponse {
        return linkMutex.withLock {
            val codeUser = redeem(code) ?: return LinkResponse.NoCode
            val linkedPlayer = findOrCreate(codeUser)
                .getOrElse { t -> return unknownError("linkTelegram", t) }
            if (linkedPlayer.telegramLink != null) return LinkResponse.AlreadyLinked
            val savedPlayer = linkingDao.upsert(linkedPlayer.copy(telegramLink = telegramLink))
                .getOrElse { t -> return unknownError("linkTelegram", t) }
            giveGroup(savedPlayer.uuid)
            LinkResponse.Linked(savedPlayer)
        }
    }

    override suspend fun revokeLeftMember(discordId: Long) {
        val link = config.link ?: return
        linkMutex.withLock {
            val linkedPlayer = linkingDao.findByDiscordId(discordId)
                .onFailure { t -> error(t) { "#revokeLeftMember could not find $discordId" } }
                .getOrNull()
                ?: return
            unlinkLeftDiscord(linkedPlayer, link)
        }
    }

    override suspend fun revokeAbsentMembers(memberIds: Set<Long>) {
        val link = config.link ?: return
        if (memberIds.isEmpty()) return
        linkMutex.withLock {
            val linkedPlayers = discordLinkedPlayerDao.findAllWithDiscordLink()
                .onFailure { t -> error(t) { "#revokeAbsentMembers could not read the linked players" } }
                .getOrNull()
                ?: return
            linkedPlayers
                .filter { linkedPlayer ->
                    val discordLink = linkedPlayer.discordLink
                    discordLink != null && discordLink.discordId !in memberIds
                }
                .forEach { linkedPlayer -> unlinkLeftDiscord(linkedPlayer, link) }
        }
    }

    override suspend fun unlink(uuid: UUID): UnlinkResponse {
        return linkMutex.withLock {
            val linkedPlayer = linkingDao.findByUuid(uuid)
                .getOrElse { t -> return unlinkFailed(uuid, t) }
                ?: return UnlinkResponse.NotLinked
            config.link?.let { link ->
                permissionGroups.remove(linkedPlayer.uuid, link.linkLuckPermsRole)
                    .onFailure { t -> return unlinkFailed(uuid, t) }
            }
            linkingDao.deleteByUuid(uuid).fold(
                onSuccess = { _ -> UnlinkResponse.Unlinked },
                onFailure = { t -> unlinkFailed(uuid, t) }
            )
        }
    }
}
