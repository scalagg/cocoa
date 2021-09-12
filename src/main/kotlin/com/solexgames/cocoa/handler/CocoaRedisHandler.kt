package com.solexgames.cocoa.handler

import com.solexgames.cocoa.CocoaProxyConstants
import com.solexgames.cocoa.CocoaProxyPlugin
import com.solexgames.redis.annotation.Subscription
import com.solexgames.redis.handler.JedisHandler
import com.solexgames.redis.json.JsonAppender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap

/**
 * @author GrowlyX
 * @since 9/12/2021
 */
object CocoaRedisHandler : JedisHandler {

    @Subscription(action = "maintenance-kick")
    fun onMaintenanceKick(jsonAppender: JsonAppender) {
        ProxyServer.getInstance().players
            .filter { !CocoaProxyPlugin.instance.whitelistedPlayers.contains(it.uniqueId) }
            .forEach { it.disconnect(CocoaProxyConstants.MAINTENANCE_MESSAGE) }
    }

    @Subscription(action = "permission-update")
    fun onPermissionUpdate(jsonAppender: JsonAppender) {
        ProxyServer.getInstance().getPlayer(
            UUID.fromString(jsonAppender.getParam("uniqueId"))
        )?.let {
            it.displayName = jsonAppender.getParam("currentDisplayName")

            jsonAppender.getParam("currentBungeePermissions").split(",").forEach { permission ->
                it.setPermission(permission, true)
            }
        }
    }

    fun sendStaffNotification(
        sender: ProxiedPlayer,
        notification: String,
    ): CompletableFuture<Void> {
        return buildMessage(
            "staff-message",
            HashMap<String, String>().also {
                it["sender-fancy"] = sender.displayName
                it["message"] = notification
                it["permission"] = "lemon.staff"
                it["messageType"] = "NOTIFICATION"

                it["server"] = "proxy"
                it["with-server"] = "false"
            }
        ).publishAsync()
    }

    fun buildMessage(packet: String, message: Map<String, String>): RedisMessage {
        return RedisMessage(JsonAppender(packet).also {
            message.forEach { (key, value) ->
                it.put(key, value)
            }
        }.asJson)
    }

    /**
     * @author puugz, GrowlyX
     * @since 27/08/2021 21:27
     */
    class RedisMessage(private val message: String) {

        fun publishAsync(): CompletableFuture<Void> {
            return CompletableFuture.runAsync {
                CocoaProxyPlugin.instance.lemonJedisManager.publish(message)
            }
        }

        fun publishAsync(channel: String): CompletableFuture<Void> {
            return CompletableFuture.runAsync {
                CocoaProxyPlugin.instance.lemonJedisManager.runCommand {
                    it.publish(channel, message)
                }
            }
        }

        fun publishSync() {
            CocoaProxyPlugin.instance.lemonJedisManager.publish(message)
        }
    }

}
