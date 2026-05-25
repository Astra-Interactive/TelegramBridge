package ru.astrainteractive.messagebridge.messenger.telegram.model

/**
 * Whether an update should be relayed at all. Each cause is its own type so callers dispatch
 * over it polymorphically instead of inspecting a free-form reason.
 */
internal sealed interface MessageRelevance {
    data object Relevant : MessageRelevance
    data object WrongChat : MessageRelevance
    data object NoDate : MessageRelevance
    data object TooOld : MessageRelevance
    data object WrongTopic : MessageRelevance
}