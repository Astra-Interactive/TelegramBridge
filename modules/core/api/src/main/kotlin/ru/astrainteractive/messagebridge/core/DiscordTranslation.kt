package ru.astrainteractive.messagebridge.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText

/** Texts about setting up the Discord bot and the errors it runs into. */
@Serializable
data class DiscordTranslation(
    @SerialName("guide")
    val guide: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            """
            Discord is not configured yet:
             1. Create an application at https://discord.com/developers/applications, open Bot, copy the token
                and turn on Message Content Intent
             2. In the server console run: mb discord token <token>
             3. Run mb discord invite and open the link to add the bot to your server
             4. Run mb discord bind and send the code it shows into the channel
             5. Check everything with mb discord check
            Guide: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/en/discord.md
            """.trimIndent()
        )
        translation(
            MinecraftLocales.RU_RU,
            """
            Discord ещё не настроен:
             1. Создайте приложение на https://discord.com/developers/applications, откройте Bot, скопируйте токен
                и включите Message Content Intent
             2. В консоли сервера выполните: mb discord token <токен>
             3. Выполните mb discord invite и откройте ссылку, чтобы добавить бота на сервер
             4. Выполните mb discord bind и отправьте код в нужный канал
             5. Проверьте настройку: mb discord check
            Инструкция: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/ru/discord.md
            """.trimIndent()
        )
    }
)
