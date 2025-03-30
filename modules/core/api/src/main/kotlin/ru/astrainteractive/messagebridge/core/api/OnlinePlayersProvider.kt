package ru.astrainteractive.messagebridge.core.api

interface OnlinePlayersProvider {
    fun provide(): List<String>
}
