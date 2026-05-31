package ru.astrainteractive.messagebridge.messenger.telegram.model

internal sealed interface MessageRelevance {
    data object Relevant : MessageRelevance
    data object WrongChat : MessageRelevance
    data object NoDate : MessageRelevance
    data object TooOld : MessageRelevance
    data object WrongTopic : MessageRelevance
}
