package ru.astrainteractive.messagebridge.messenger.telegram.fake

import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation

internal class FakeTranslationKrate(
    @Volatile var translation: PluginTranslation
) : CachedKrate<PluginTranslation> {
    override val cachedValue: PluginTranslation
        get() = translation

    override fun getValue(): PluginTranslation = translation
}
