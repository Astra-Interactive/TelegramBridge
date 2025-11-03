package ru.astrainteractive.messagebridge.commands.link

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import ru.astrainteractive.astralibs.command.api.argumenttype.ArgumentConverter
import ru.astrainteractive.astralibs.command.api.exception.CommandException

internal object OfflinePlayerArgumentType : ArgumentConverter<OfflinePlayer> {
    class PlayerNotFoundException(name: String) : CommandException("Player $name not found")

    override fun transform(argument: String): OfflinePlayer {
        val offlinePlayer = Bukkit.getOfflinePlayer(argument)
        if (!offlinePlayer.hasPlayedBefore()) throw PlayerNotFoundException(argument)
        return offlinePlayer
    }
}
