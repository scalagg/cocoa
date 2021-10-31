package gg.scala.cocoa.vpn

import com.google.gson.annotations.SerializedName

/**
 * Represents the callback from the
 * request sent to `vpnblocker.net's` api.
 *
 * @author GrowlyX
 * @since 10/31/2021
 */
data class VpnRequestModel(
    val status: String
)
{
    @SerializedName("host-ip")
    val shouldAllow: Boolean = false

    @SerializedName("org")
    val networkId: String = "N/A"
}
