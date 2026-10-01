package ru.astrainteractive.messagebridge.onboarding.fake

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers

internal class FakeKotlinDispatchers(dispatcher: CoroutineDispatcher) : KotlinDispatchers {
    override val Main: MainCoroutineDispatcher
        get() = error("The /mb commands never run on the main thread")
    override val IO: CoroutineDispatcher = dispatcher
    override val Default: CoroutineDispatcher = dispatcher
    override val Unconfined: CoroutineDispatcher = dispatcher
}
