package ru.astrainteractive.messagebridge.core

import kotlinx.serialization.Serializable

@Serializable
enum class ProxyType { HTTP, SOCKS5 }
