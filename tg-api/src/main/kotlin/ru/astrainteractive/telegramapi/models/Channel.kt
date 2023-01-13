package ru.astrainteractive.telegramapi.models

sealed class Channel(val id: Long) {
    class TopicChannel(id: Long) : Channel(id)
    class Group(id: Long) : Channel(id)
}