package ru.astrainteractive.messagebridge.core.api.api

interface OnlinePlayersProvider {
    fun provide(): List<String>
}
