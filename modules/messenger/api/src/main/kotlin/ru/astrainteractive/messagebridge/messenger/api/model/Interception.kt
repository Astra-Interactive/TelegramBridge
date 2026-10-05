package ru.astrainteractive.messagebridge.messenger.api.model

sealed interface Interception {
    data object Pass : Interception
    data object Consumed : Interception
    data class Reply(val text: String) : Interception
}
