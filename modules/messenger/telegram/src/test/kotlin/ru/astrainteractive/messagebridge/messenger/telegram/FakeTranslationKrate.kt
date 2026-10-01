package ru.astrainteractive.messagebridge.messenger.telegram

import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.messagebridge.core.PluginTranslation

/** translations.yml in memory; [translation] changes the way a reload does. */
internal class FakeTranslationKrate(
    @Volatile var translation: PluginTranslation
) : CachedKrate<PluginTranslation> {
    override val cachedValue: PluginTranslation
        get() = translation

    override fun getValue(): PluginTranslation = translation
}
