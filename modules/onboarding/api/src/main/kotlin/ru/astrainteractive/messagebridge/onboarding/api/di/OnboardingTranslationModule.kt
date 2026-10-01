package ru.astrainteractive.messagebridge.onboarding.api.di

import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.util.parseOrWriteIntoDefault
import ru.astrainteractive.klibs.kstorage.api.StateFlowKrate
import ru.astrainteractive.klibs.kstorage.api.asStateFlowKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation

class OnboardingTranslationModule(coreModule: CoreModule) {
    val translationKrate: StateFlowKrate<OnboardingTranslation> = DefaultMutableKrate(
        factory = ::OnboardingTranslation,
        loader = {
            coreModule.yamlStringFormat.parseOrWriteIntoDefault(
                file = coreModule.translationFolder.resolve(FILE_NAME),
                logger = JUtiltLogger("MessageBridge-onboarding-translations"),
                default = ::OnboardingTranslation
            )
        }
    ).asStateFlowKrate()

    val lifecycle = Lifecycle.Lambda(
        onReload = { translationKrate.getValue() }
    )

    private companion object {
        const val FILE_NAME = "onboarding.yml"
    }
}
