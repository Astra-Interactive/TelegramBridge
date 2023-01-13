package ru.astrainteractive.telegrambridge.events

import ru.astrainteractive.telegrambridge.events.events.MultipleEventsDSL
import ru.astrainteractive.astralibs.events.EventListener
import ru.astrainteractive.astralibs.events.EventManager


/**
 * Handler for all your events
 */
class EventHandler : EventManager {
    override val handlers: MutableList<EventListener> = mutableListOf()
    private val handler: EventHandler = this

    init {
        MultipleEventsDSL()
    }
}
