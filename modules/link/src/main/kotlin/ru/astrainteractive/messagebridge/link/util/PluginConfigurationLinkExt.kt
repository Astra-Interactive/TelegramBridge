package ru.astrainteractive.messagebridge.link.util

import ru.astrainteractive.messagebridge.core.api.config.model.PluginConfiguration

internal fun PluginConfiguration.Link.discordRoleIdOrNull(): Long? = linkDiscordRole.toLongOrNull()
