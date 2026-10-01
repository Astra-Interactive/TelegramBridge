package ru.astrainteractive.messagebridge.core.api.config

import kotlinx.serialization.StringFormat
import ru.astrainteractive.astralibs.util.krateOf
import ru.astrainteractive.klibs.kstorage.api.StateFlowKrate
import ru.astrainteractive.klibs.kstorage.api.asStateFlowKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.util.describe
import java.io.File

inline fun <reified T> StringFormat.translationKrateOf(
    file: File,
    noinline factory: () -> T,
    logger: Logger,
): StateFlowKrate<T> {
    val fileKrate = krateOf(file = file, factory = factory)
    return DefaultMutableKrate(
        factory = factory,
        loader = {
            fileKrate.getValue()
                .onFailure { error ->
                    val name = "${file.parentFile.name}/${file.name}"
                    logger.error { "$name has an error, the default texts are used: ${error.describe()}" }
                }
                .getOrNull()
        }
    ).asStateFlowKrate()
}
