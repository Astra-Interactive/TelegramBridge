package ru.astrainteractive.messagebridge.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.string.StringDesc
import ru.astrainteractive.astralibs.string.replace

@Serializable
data class PluginTranslation(
    @SerialName("general.prefix")
    val prefix: StringDesc.Raw = StringDesc.Raw("&#18dbd1[EmpireItems]"),
    @SerialName("general.reload")
    val reload: StringDesc.Raw = StringDesc.Raw("&#dbbb18Перезагрузка плагина"),
    @SerialName("general.reload_complete")
    val reloadComplete: StringDesc.Raw = StringDesc.Raw("&#42f596Перезагрузка успешно завершена"),
    @SerialName("general.no_permission")
    val noPermission: StringDesc.Raw = StringDesc.Raw("&#db2c18У вас нет прав!"),
    @SerialName("messaging.player_join")
    private val playerJoinMessage: StringDesc.Raw = StringDesc.Raw("Игрок %player% присоединился"),
    @SerialName("messaging.player_join_first_time")
    private val playerJoinMessageFirstTime: StringDesc.Raw = StringDesc.Raw(
        "\uD83E\uDD73 Игрок %player% присоединился впервые!"
    ),
    @SerialName("messaging.player_leave")
    private val playerLeaveMessage: StringDesc.Raw = StringDesc.Raw("Игрок %player% покинул нас"),
    @SerialName("messaging.player_died")
    private val playerDiedMessage: StringDesc.Raw = StringDesc.Raw("Игрок %player% сдох от %cause%"),
    @SerialName("messaging.message.to_telegram")
    private val telegramMessageFormat: StringDesc.Raw = StringDesc.Raw("[%from%] %player%:\n%message%"),
    @SerialName("messaging.message.to_minecraft")
    private val minecraftMessageFormat: StringDesc.Raw = StringDesc.Raw("[%from%] &#27A1E0%player%: &#FFFFFF%message%"),
    @SerialName("messaging.message.server_open")
    val serverOpenMessage: StringDesc.Raw = StringDesc.Raw("✅ Сервер успешно запущен"),
    @SerialName("messaging.message.server_closed")
    val serverClosedMessage: StringDesc.Raw = StringDesc.Raw("\uD83D\uDED1 Сервер остановлен"),
    @SerialName("link")
    val link: Link = Link()
) {
    @Serializable
    data class Link(
        @SerialName("code_created")
        private val codeCreated: StringDesc.Raw = StringDesc.Raw(
            "&#42f596Ваш код: %code%. Используйте /link <code> в TG или Discord."
        ),
        @SerialName("already_linked")
        val alreadyLinked: StringDesc.Raw = StringDesc.Raw("Вы уже привязали аккаунт этим способом"),
        @SerialName("no_code_found")
        val noCodeFound: StringDesc.Raw = StringDesc.Raw("Код не найден. Используйте /link в игре для создания кода"),
        @SerialName("unknown_error")
        val unknownError: StringDesc.Raw = StringDesc.Raw("Произошла неизвестная ошибка"),
        @SerialName("link_success")
        val linkSuccess: StringDesc.Raw = StringDesc.Raw("Привязка прошла успешно"),
    ) {
        fun codeCreated(code: Int) = codeCreated.replace("%code%", "$code")
    }

    fun minecraftMessageFormat(
        playerName: String,
        message: String,
        from: String
    ) = minecraftMessageFormat
        .replace("%player%", playerName)
        .replace("%message%", message)
        .replace("%from%", from)

    fun telegramMessageFormat(
        playerName: String,
        message: String,
        from: String
    ) = telegramMessageFormat
        .replace("%player%", playerName)
        .replace("%message%", message)
        .replace("%from%", from)

    fun playerDiedMessage(name: String, cause: String?) = playerDiedMessage
        .replace("%player%", name)
        .replace("%cause%", cause ?: "Просто так")

    fun playerLeaveMessage(name: String) = playerLeaveMessage
        .replace("%player%", name)

    fun playerJoinMessage(name: String) = playerJoinMessage
        .replace("%player%", name)

    fun playerJoinMessageFirstTime(name: String) = playerJoinMessageFirstTime
        .replace("%player%", name)
}
