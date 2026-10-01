package ru.astrainteractive.messagebridge.messenger.telegram.connection

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

/**
 * Keeps only the IPv4 addresses of a host that has them: on a server without IPv6 routing every IPv6 address of
 * Telegram costs a whole connect timeout before the next one is tried.
 */
internal class Ipv4FirstDns(
    private val delegate: Dns,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val resolved = delegate.lookup(hostname)
        return resolved.filterIsInstance<Inet4Address>().ifEmpty { resolved }
    }
}
