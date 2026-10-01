package ru.astrainteractive.messagebridge.onboarding.api.di

import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.kstorage.api.StateFlowKrate
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.messagebridge.core.api.config.translationKrateOf
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation

class OnboardingTranslationModule(coreModule: CoreModule) {
    val translationKrate: StateFlowKrate<OnboardingTranslation> = coreModule.yamlStringFormat.translationKrateOf(
        file = coreModule.translationFolder.directory.resolve(FILE_NAME),
        factory = ::OnboardingTranslation,
        logger = JUtiltLogger("MessageBridge-onboarding-translations")
    )

    val lifecycle = Lifecycle.Lambda(
        onReload = { translationKrate.getValue() }
    )

    private companion object {
        const val FILE_NAME = "onboarding.yml"
    }
}
