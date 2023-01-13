package ru.astrainteractive.telegramapi

class BotApi(
    private val token: String,
    private val client: HttpClient
) {
    suspend fun getMe(): Boolean {
        val url = Endpoint.GetMe(token).url
        return client.get(url).isSuccess
    }

    suspend fun getUpdate(): Result<String> {
        val url = Endpoint.GetUpdates(token, 0).url
        return client.post(url)
    }
}