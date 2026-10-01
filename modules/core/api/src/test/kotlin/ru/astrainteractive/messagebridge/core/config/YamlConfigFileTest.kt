@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.core.config

import com.charleskorn.kaml.Yaml
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YamlConfigFileTest {
    private val folder: File = Files.createTempDirectory("config").toFile()
    private val file = folder.resolve("config.yml")
    private val yaml = Yaml(configuration = Yaml.default.configuration.copy(encodeDefaults = true, strictMode = false))
    private val configFile = YamlConfigFile(
        stringFormat = yaml,
        serializer = PluginConfiguration.serializer(),
        file = file,
        factory = ::PluginConfiguration
    )

    @AfterTest
    fun cleanup() {
        folder.deleteRecursively()
    }

    @Test
    fun GIVEN_no_file_WHEN_loaded_THEN_defaults_are_written() {
        assertEquals(PluginConfiguration(), configFile.load())
        assertTrue(file.readText().contains("tgConfig"))
    }

    @Test
    fun GIVEN_file_with_an_error_WHEN_reloaded_THEN_previous_settings_are_kept() {
        val working = PluginConfiguration(tgConfig = PluginConfiguration.TelegramConfig(token = "123:abc"))
        configFile.save(working)
        configFile.load()
        file.writeText("tgConfig:\n  max_telegram_message_length: not-a-number\n")

        val reloaded = configFile.load()

        assertEquals(working, reloaded)
        assertNotNull(configFile.lastError)
        assertEquals("tgConfig:\n  max_telegram_message_length: not-a-number\n", file.readText())
    }

    @Test
    fun GIVEN_fixed_file_WHEN_reloaded_THEN_error_is_cleared() {
        file.writeText("displayJoinMessage: [")
        configFile.load()
        configFile.save(PluginConfiguration(displayJoinMessage = false))

        assertEquals(false, configFile.load().displayJoinMessage)
        assertNull(configFile.lastError)
    }

    @Test
    fun GIVEN_krate_WHEN_value_is_saved_THEN_file_has_it() {
        val krate = configFile.krate()

        krate.save { it.copy(displayDeathMessage = false) }

        assertEquals(false, configFile.parse().getOrThrow().displayDeathMessage)
        assertEquals(false, krate.cachedValue.displayDeathMessage)
    }
}
