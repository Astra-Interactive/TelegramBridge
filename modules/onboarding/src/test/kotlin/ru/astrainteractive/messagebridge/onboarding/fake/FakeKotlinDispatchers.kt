package ru.astrainteractive.messagebridge.onboarding.fake

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers

/** Runs every coroutine of the plugin on [dispatcher], so a test controls the time the commands wait. */
internal class FakeKotlinDispatchers(dispatcher: CoroutineDispatcher) : KotlinDispatchers {
    override val Main: MainCoroutineDispatcher
        get() = error("The /mb commands never run on the main thread")
    override val IO: CoroutineDispatcher = dispatcher
    override val Default: CoroutineDispatcher = dispatcher
    override val Unconfined: CoroutineDispatcher = dispatcher
}
