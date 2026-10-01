package ru.astrainteractive.messagebridge.link.player.fake

import kotlinx.coroutines.yield
import ru.astrainteractive.messagebridge.link.player.api.DiscordLinkedPlayerDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayerModel

internal class FakeDiscordLinkedPlayerDao(
    private val linkingDao: FakeLinkingDao
) : DiscordLinkedPlayerDao {
    override suspend fun findAllWithDiscordLink(): Result<List<LinkedPlayerModel>> {
        yield()
        linkingDao.failure?.let { error -> return Result.failure(error) }
        return Result.success(linkingDao.players.values.filter { player -> player.discordLink != null })
    }
}
