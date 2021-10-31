package gg.scala.cocoa.vpn

import gg.scala.cocoa.CocoaProxyPlugin
import net.evilblock.cubed.serializers.Serializers
import java.io.InputStreamReader
import java.net.URL

/**
 * @author GrowlyX
 * @since 10/31/2021
 */
object CocoaVpnHandler
{
    @JvmStatic
    private val API_KEY by lazy {
        CocoaProxyPlugin.instance.config
            .getString("vpn-blocker.api-key")
    }

    @JvmStatic
    val REQUEST_URL = "https://api.vpnblocker.net/v2/json/%s/$API_KEY"

    /**
     * Requests host data for an ip address.
     *
     * @param ipAddress the ip address in string form
     * @return nullable [VpnRequestModel]
     */
    fun fetch(ipAddress: String): VpnRequestModel?
    {
        val url = URL(String.format(REQUEST_URL, ipAddress))

        return Serializers.gson.fromJson(
            InputStreamReader(url.openStream()), VpnRequestModel::class.java
        )
    }
}
