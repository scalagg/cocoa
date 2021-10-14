package gg.scala.cocoa.handler

import gg.scala.banana.annotate.Subscribe
import gg.scala.banana.message.Message
import gg.scala.banana.subscribe.marker.BananaHandler
import gg.scala.cocoa.CocoaProxyConstants
import gg.scala.cocoa.CocoaProxyPlugin
import gg.scala.cocoa.dispatchToLemon
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.*
import java.util.concurrent.ForkJoinPool

/**
 * @author GrowlyX
 * @since 9/12/2021
 */
object CocoaRedisHandler : BananaHandler {

   @Subscribe("maintenance-kick")
    fun onMaintenanceKick(message: Message) {
        ProxyServer.getInstance().players
            .filter { !CocoaProxyPlugin.instance.whitelistedPlayers.contains(it.uniqueId) }
            .forEach { it.disconnect(CocoaProxyConstants.MAINTENANCE_MESSAGE) }
    }

   @Subscribe("permission-update")
    fun onPermissionUpdate(message: Message) {
        ProxyServer.getInstance().getPlayer(
            UUID.fromString(message["uniqueId"])
        )?.let {
            it.displayName = message["currentDisplayName"]

            message["currentBungeePermissions"]!!.split(",").forEach { permission ->
                it.setPermission(permission, true)
            }
        }
    }

    fun sendStaffNotification(
        sender: ProxiedPlayer,
        notification: String,
    ) {
        ForkJoinPool.commonPool().execute {
            buildMessage(
                "staff-message",
                HashMap<String, String>().also {
                    it["sender-fancy"] = sender.displayName
                    it["message"] = notification
                    it["permission"] = "lemon.staff"
                    it["message-type"] = "NOTIFICATION"

                    it["server"] = "proxy"
                    it["with-server"] = "false"
                }
            ).dispatchToLemon()
        }
    }

    fun buildMessage(packet: String, message: Map<String, String>): Message {
        return Message(packet).also {
            message.forEach { (key, value) ->
                it[key] = value
            }
        }
    }
}
