@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network

import okhttp3.Dns
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals

class Ipv4FirstDnsTest {
    private val ipv4 = InetAddress.getByName("149.154.167.220")
    private val ipv6 = InetAddress.getByName("2001:67c:4e8:f004::9")

    private fun dnsResolving(vararg addresses: InetAddress) = Ipv4FirstDns(
        delegate = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = addresses.toList()
        }
    )

    @Test
    fun GIVEN_host_with_both_families_WHEN_looked_up_THEN_only_ipv4_is_tried() {
        assertEquals(listOf(ipv4), dnsResolving(ipv6, ipv4).lookup("api.telegram.org"))
    }

    @Test
    fun GIVEN_host_with_only_ipv6_WHEN_looked_up_THEN_ipv6_is_kept() {
        assertEquals(listOf(ipv6), dnsResolving(ipv6).lookup("api.telegram.org"))
    }
}
