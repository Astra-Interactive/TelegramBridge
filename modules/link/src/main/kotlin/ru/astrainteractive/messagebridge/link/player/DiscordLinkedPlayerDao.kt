package ru.astrainteractive.messagebridge.link.player

/** Kept apart from [LinkingDao], whose clients only look single players up. */
internal interface DiscordLinkedPlayerDao {
    suspend fun findAllWithDiscordLink(): Result<List<LinkedPlayerModel>>
}
