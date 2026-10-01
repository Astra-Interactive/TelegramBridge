@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.impl.di

import ru.astrainteractive.messagebridge.onboarding.impl.fake.OnboardingFixture
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class OnboardingModuleTest {
    private val fixture = OnboardingFixture()

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    @Test
    fun GIVEN_enabled_module_WHEN_server_accepts_commands_THEN_mb_stays_registered_while_the_plugin_runs() {
        val registration = fixture.registrar.registrations.single()

        assertEquals("mb", registration.node.literal)
        assertSame(fixture.coreModule.unconfinedScope, registration.scope)
    }
}
