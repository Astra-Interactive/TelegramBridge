@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.core.api.config

import com.charleskorn.kaml.Yaml
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigErrorDescriptionTest {
    private val yaml = Yaml(configuration = Yaml.default.configuration.copy(encodeDefaults = true, strictMode = false))

    private fun errorOf(text: String): Throwable = runCatching {
        yaml.decodeFromString(PluginConfiguration.serializer(), text)
    }.exceptionOrNull() ?: error("$text was parsed")

    @Test
    fun GIVEN_malformed_yaml_WHEN_described_THEN_names_the_line_and_column() {
        val malformed = """
            tgConfig:
              token: "123"
              broken: [
        """.trimIndent()

        val description = describeConfigError(errorOf(malformed))

        assertTrue(description.startsWith("line "), description)
        assertTrue("column" in description, description)
    }

    @Test
    fun GIVEN_value_of_wrong_type_WHEN_described_THEN_names_the_line() {
        val description = describeConfigError(errorOf("tgConfig:\n  max_telegram_message_length: not-a-number\n"))

        assertTrue(description.startsWith("line 2"), description)
    }

    @Test
    fun GIVEN_error_that_is_not_from_yaml_WHEN_described_THEN_is_its_message() {
        assertEquals("disk is full", describeConfigError(IllegalStateException("disk is full")))
    }

    @Test
    fun GIVEN_error_without_message_WHEN_described_THEN_is_its_class_name() {
        assertEquals("IllegalStateException", describeConfigError(IllegalStateException()))
    }
}
