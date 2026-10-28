package ru.astrainteractive.messagebridge.messenger.discord.util

import net.dv8tion.jda.api.requests.RestAction
import ru.astrainteractive.messagebridge.messenger.api.discord.util.await as apiAwait

suspend fun <T> RestAction<T>.await(): T = apiAwait()
