package ru.astrainteractive.telegramapi

sealed class Endpoint(val url: String) {
    companion object {
        const val BASE_URL = "https://api.telegram.org/bot"
    }

    class GetMe(token: String) : Endpoint("$BASE_URL$token/getMe")
    class GetUpdates(token: String, offset: Long) : Endpoint("$BASE_URL$token/getUpdates")
}