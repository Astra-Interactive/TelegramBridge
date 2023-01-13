package ru.astrainteractive.telegramapi

class TelegramBot(private val token: String) {
    private val client: HttpClient = HttpClient()
    private val botApi = BotApi(token, client)
    suspend fun connect() = botApi.getMe()
}

