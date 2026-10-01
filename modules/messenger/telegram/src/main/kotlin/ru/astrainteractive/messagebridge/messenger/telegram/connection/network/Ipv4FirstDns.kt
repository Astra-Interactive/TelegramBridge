package ru.astrainteractive.messagebridge.messenger.telegram.connection.network

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

internal class Ipv4FirstDns(
    private val delegate: Dns,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val resolved = delegate.lookup(hostname)
        return resolved.filterIsInstance<Inet4Address>().ifEmpty { resolved }
    }
}
