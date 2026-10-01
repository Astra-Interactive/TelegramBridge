package ru.astrainteractive.messagebridge.link

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.entities.Member
import org.telegram.telegrambots.meta.api.objects.User
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.link.code.CodeApi
import ru.astrainteractive.messagebridge.link.code.CodeUser
import ru.astrainteractive.messagebridge.link.player.DiscordLinkedPlayerDao
import ru.astrainteractive.messagebridge.link.player.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.player.LinkingDao
import ru.astrainteractive.messagebridge.link.role.DiscordLinkRole
import ru.astrainteractive.messagebridge.link.role.PermissionGroups
import java.util.UUID

internal class LinkApiImpl(
    private val linkingDao: LinkingDao,
    private val discordLinkedPlayerDao: DiscordLinkedPlayerDao,
    private val codeApi: CodeApi,
    private val discordLinkRole: DiscordLinkRole,
    private val permissionGroups: PermissionGroups,
    private val configFlow: StateFlow<PluginConfiguration>,
) : LinkApi,
    DiscordMembership,
    Logger by JUtiltLogger("MessageBridge-LinkApi") {
    private val config: PluginConfiguration
        get() = configFlow.value

    /** One change of links at a time, so a member who leaves while linking Telegram keeps a consistent record. */
    private val linkMutex = Mutex()

    /** Takes the code, so it links only once, and returns the player it was made for. */
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
            .onFailure { failure -> error(failure) { "#giveGroup could not give ${link.linkLuckPermsRole} to $uuid" } }
    }

    /** Takes the group back first, so a failure leaves the link for the next try. */
    private suspend fun unlinkLeftDiscord(linkedPlayer: LinkedPlayerModel, link: PluginConfiguration.Link) {
        val uuid = linkedPlayer.uuid
        if (linkedPlayer.telegramLink == null) {
            permissionGroups.remove(uuid, link.linkLuckPermsRole).onFailure { failure ->
                error(failure) { "#unlinkLeftDiscord could not take ${link.linkLuckPermsRole} from $uuid" }
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
            .onFailure { failure -> error(failure) { "#unlinkLeftDiscord could not unlink Discord of $uuid" } }
    }

    override suspend fun linkDiscord(code: Int, member: Member): LinkResponse {
        return linkMutex.withLock {
            val codeUser = redeem(code) ?: return LinkResponse.NoCode
            val linkedPlayer = findOrCreate(codeUser)
                .getOrElse { failure -> return unknownError("linkDiscord", failure) }
            if (linkedPlayer.discordLink != null) return LinkResponse.AlreadyLinked
            val discordLink = LinkedPlayerModel.DiscordLink(
                lastDiscordName = member.effectiveName,
                discordId = member.idLong
            )
            val savedPlayer = linkingDao.upsert(linkedPlayer.copy(discordLink = discordLink))
                .getOrElse { failure -> return unknownError("linkDiscord", failure) }
            config.link?.let { link -> discordLinkRole.give(member, link.linkDiscordRole) }
            giveGroup(savedPlayer.uuid)
            LinkResponse.Linked(savedPlayer)
        }
    }

    override suspend fun linkTelegram(code: Int, tgUser: User): LinkResponse {
        val username = tgUser.userName ?: return LinkResponse.NoUsername
        return linkMutex.withLock {
            val codeUser = redeem(code) ?: return LinkResponse.NoCode
            val linkedPlayer = findOrCreate(codeUser)
                .getOrElse { failure -> return unknownError("linkTelegram", failure) }
            if (linkedPlayer.telegramLink != null) return LinkResponse.AlreadyLinked
            val telegramLink = LinkedPlayerModel.TelegramLink(telegramUsername = username, telegramId = tgUser.id)
            val savedPlayer = linkingDao.upsert(linkedPlayer.copy(telegramLink = telegramLink))
                .getOrElse { failure -> return unknownError("linkTelegram", failure) }
            giveGroup(savedPlayer.uuid)
            LinkResponse.Linked(savedPlayer)
        }
    }

    override suspend fun revokeLeftMember(discordId: Long) {
        val link = config.link ?: return
        linkMutex.withLock {
            val linkedPlayer = linkingDao.findByDiscordId(discordId)
                .onFailure { failure -> error(failure) { "#revokeLeftMember could not find $discordId" } }
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
                .onFailure { failure -> error(failure) { "#revokeAbsentMembers could not read the linked players" } }
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
}
