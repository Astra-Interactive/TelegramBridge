package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.fake

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.concurrent.CopyOnWriteArrayList

internal class RecordingUpdateConsumer : LongPollingSingleThreadUpdateConsumer {
    val updates: MutableList<Update> = CopyOnWriteArrayList()

    override fun consume(update: Update) {
        updates += update
    }
}
