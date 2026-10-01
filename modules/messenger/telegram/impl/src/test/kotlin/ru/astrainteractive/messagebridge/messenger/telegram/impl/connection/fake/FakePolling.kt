package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.fake

internal class FakePolling : AutoCloseable {
    @Volatile
    var isClosed = false
        private set

    override fun close() {
        isClosed = true
    }
}
