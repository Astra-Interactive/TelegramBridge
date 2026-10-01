@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.core.api.config

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranslationFolderTest {
    private val dataFolder: File = createTempDirectory("translation-folder").toFile()
    private val legacyFile: File = dataFolder.resolve("translations.yml")
    private val translationFolder = TranslationFolder(dataFolder)

    @Test
    fun GIVEN_texts_in_translations_yml_WHEN_folder_is_prepared_THEN_they_move_to_main_yml() {
        legacyFile.writeText("chat: edited")

        translationFolder.prepare()

        assertEquals("chat: edited", translationFolder.mainFile.readText())
        assertFalse(legacyFile.exists())
    }

    @Test
    fun GIVEN_main_yml_already_exists_WHEN_folder_is_prepared_THEN_both_files_stay_as_they_are() {
        translationFolder.directory.mkdirs()
        translationFolder.mainFile.writeText("chat: new")
        legacyFile.writeText("chat: old")

        translationFolder.prepare()

        assertEquals("chat: new", translationFolder.mainFile.readText())
        assertEquals("chat: old", legacyFile.readText())
    }

    @Test
    fun GIVEN_new_server_WHEN_folder_is_prepared_THEN_folder_exists_without_files() {
        translationFolder.prepare()

        assertTrue(translationFolder.directory.isDirectory)
        assertFalse(translationFolder.mainFile.exists())
    }
}
