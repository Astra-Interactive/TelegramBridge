package ru.astrainteractive.messagebridge.messaging.model

sealed interface Interception {
    data object Pass : Interception
    data object Consumed : Interception
    data class Reply(val text: String) : Interception
}
