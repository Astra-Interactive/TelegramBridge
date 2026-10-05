package ru.astrainteractive.messagebridge.link.dao.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.klibs.mikro.exposed.model.DatabaseConfiguration
import ru.astrainteractive.klibs.mikro.exposed.util.connectAsFlow
import ru.astrainteractive.messagebridge.link.dao.api.LinkingDao
import ru.astrainteractive.messagebridge.link.dao.internal.LinkingDaoImpl
import ru.astrainteractive.messagebridge.link.dao.table.DiscordAccountTable
import ru.astrainteractive.messagebridge.link.dao.table.PlayerTable
import ru.astrainteractive.messagebridge.link.dao.table.TelegramAccountTable
import java.io.File
import java.sql.Connection
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class LinkDatabaseModule(
    ioScope: CoroutineScope,
    dataFolder: File
) : Logger by JUtiltLogger("MessageBridge-LinkDatabaseModule") {
    private val databaseConfig = DatabaseConfig {
        defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        defaultMaxAttempts = MAX_ATTEMPTS
        defaultMinRetryDelay = MIN_RETRY_DELAY.inWholeMilliseconds
        defaultMaxRetryDelay = MAX_RETRY_DELAY.inWholeMilliseconds
    }

    private val databaseState: StateFlow<Database?> =
        flowOf(DatabaseConfiguration.H2(dataFolder.resolve("linking").absolutePath))
            .flatMapLatest { databaseConfiguration -> databaseConfiguration.connectAsFlow(databaseConfig) }
            .onEach { database ->
                transaction(database) {
                    maxAttempts = 1
                    SchemaUtils.create(PlayerTable, DiscordAccountTable, TelegramAccountTable)
                }
            }
            .retryWhen { t, attempt ->
                val retryDelay = (OPEN_RETRY_DELAY * 2.0.pow(attempt.toInt())).coerceAtMost(MAX_OPEN_RETRY_DELAY)
                warn { "#databaseState could not open the link database, retrying in $retryDelay: ${t.message}" }
                delay(retryDelay)
                true
            }
            .stateIn(ioScope, SharingStarted.Eagerly, null)

    val linkingDao: LinkingDao = LinkingDaoImpl(databaseState)

    private companion object {
        const val MAX_ATTEMPTS = 3
        val MIN_RETRY_DELAY = 100.milliseconds
        val MAX_RETRY_DELAY = 1.seconds
        val OPEN_RETRY_DELAY = 1.seconds
        val MAX_OPEN_RETRY_DELAY = 1.minutes
    }
}
