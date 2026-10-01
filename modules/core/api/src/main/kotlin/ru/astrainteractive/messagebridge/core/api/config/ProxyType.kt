package ru.astrainteractive.messagebridge.core.api.config

import kotlinx.serialization.Serializable

@Serializable
enum class ProxyType { HTTP, SOCKS5 }
