import ru.astrainteractive.astralibs.di.getValue
import ru.astrainteractive.telegrambridge.commands.*
import ru.astrainteractive.telegrambridge.modules.Modules


/**
 * Command handler for your plugin
 * It's better to create different executors for different commands
 * @see Reload
 */
class CommandManager {
    val translation by Modules.TranslationModule

    /**
     * Here you should declare commands for your plugin
     *
     * Commands stored in plugin.yml
     *
     * etemp has TabCompleter
     */
    init {
        tabCompleter()
        reload()
    }


}