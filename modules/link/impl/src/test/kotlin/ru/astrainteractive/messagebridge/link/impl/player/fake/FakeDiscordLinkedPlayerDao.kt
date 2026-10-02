package ru.astrainteractive.messagebridge.link.impl.player.fake

import kotlinx.coroutines.yield
import ru.astrainteractive.messagebridge.link.api.player.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.impl.player.api.DiscordLinkedPlayerDao

internal class FakeDiscordLinkedPlayerDao(
    private val linkingDao: FakeLinkingDao
) : DiscordLinkedPlayerDao {
    override suspend fun findAllWithDiscordLink(): Result<List<LinkedPlayerModel>> {
        yield()
        linkingDao.failure?.let { t -> return Result.failure(t) }
        return Result.success(linkingDao.players.values.filter { player -> player.discordLink != null })
    }
}
