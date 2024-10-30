package ru.astrainteractive.messagebridge

interface OnlinePlayersProvider {
    fun provide(): List<String>
}
