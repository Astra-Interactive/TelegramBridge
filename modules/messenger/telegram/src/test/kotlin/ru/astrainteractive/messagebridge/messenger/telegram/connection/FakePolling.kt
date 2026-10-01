package ru.astrainteractive.messagebridge.messenger.telegram.connection

internal class FakePolling : AutoCloseable {
    @Volatile
    var isClosed = false
        private set

    override fun close() {
        isClosed = true
    }
}
