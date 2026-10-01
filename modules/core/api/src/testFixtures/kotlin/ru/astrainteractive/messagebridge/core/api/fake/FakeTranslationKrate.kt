package ru.astrainteractive.messagebridge.core.api.fake

import ru.astrainteractive.klibs.kstorage.api.CachedKrate

class FakeTranslationKrate<T>(
    @Volatile var translation: T
) : CachedKrate<T> {
    override val cachedValue: T
        get() = translation

    override fun getValue(): T = translation
}
