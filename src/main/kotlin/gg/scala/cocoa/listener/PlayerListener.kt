package gg.scala.cocoa.listener

import gg.scala.cocoa.CocoaProxyConstants
import gg.scala.cocoa.CocoaProxyPlugin
import gg.scala.cocoa.handler.CocoaRedisHandler
import net.md_5.bungee.api.AbstractReconnectHandler
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.event.*
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 9/12/2021
 */
class PlayerListener(
    private val plugin: CocoaProxyPlugin
) : Listener
{

    @EventHandler
    fun onPing(event: ProxyPingEvent)
    {
        event.response ?: return
        val response = event.response.version

        response.protocol = if (plugin.maintenance) -1 else response.protocol
        response.name = if (plugin.maintenance) "Whitelisted" else response.name

        event.response.description = plugin.fetchBestMessage()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onLogin(event: LoginEvent)
    {
        val uniqueId = event.connection.uniqueId

        if (plugin.maintenance && !plugin.whitelistedPlayers.contains(uniqueId))
        {
            event.cancelReason = CocoaProxyConstants.MAINTENANCE_MESSAGE
            event.isCancelled = true
            return
        }
    }

    @EventHandler
    fun onLeave(event: ServerDisconnectEvent)
    {
        if (event.player.hasPermission("cocoa.staff"))
        {
            ProxyServer.getInstance().scheduler.schedule(CocoaProxyPlugin.instance, {
                val proxiedPlayer = ProxyServer.getInstance().getPlayer(event.player.uniqueId)

                if (proxiedPlayer == null)
                {
                    CocoaRedisHandler.sendStaffNotification(
                        event.player,
                        "${ChatColor.AQUA}${event.player.displayName}${ChatColor.DARK_AQUA} disconnected from ${ChatColor.AQUA}${event.target.name}${ChatColor.DARK_AQUA}."
                    )
                }
            }, 2L, TimeUnit.MILLISECONDS)
        }
    }

    @EventHandler
    fun onSwitch(event: ServerSwitchEvent)
    {
        if (event.from != null && event.player.hasPermission("cocoa.staff"))
        {
            CocoaRedisHandler.sendStaffNotification(
                event.player,
                "${ChatColor.AQUA}${event.player.displayName}${ChatColor.DARK_AQUA} joined ${ChatColor.AQUA}${event.player.server.info.name}${ChatColor.DARK_AQUA} from ${ChatColor.AQUA}${event.from.name}${ChatColor.DARK_AQUA}."
            )
        }
    }

    @EventHandler
    fun onServerConnect(event: ServerConnectEvent)
    {
        if (event.reason == ServerConnectEvent.Reason.JOIN_PROXY)
        {
            event.target = (if (event.player.hasPermission("cocoa.staff"))
            {
                plugin.fetchBestRestrictedLobby()
            } else plugin.fetchBestLobby()) ?: event.target

            ProxyServer.getInstance().scheduler.schedule(CocoaProxyPlugin.instance, {
                val proxiedPlayer = ProxyServer.getInstance().getPlayer(event.player.uniqueId)

                if (proxiedPlayer != null && proxiedPlayer.hasPermission("cocoa.staff"))
                {
                    CocoaRedisHandler.sendStaffNotification(
                        event.player,
                        "${ChatColor.AQUA}${event.player.displayName}${ChatColor.DARK_AQUA} connected to ${ChatColor.AQUA}${event.target.name}${ChatColor.DARK_AQUA}."
                    )
                }
            }, 1L, TimeUnit.SECONDS)
        } else
        {
            if (!event.target.canAccess(event.player))
            {
                event.player.sendMessage("${ChatColor.RED}Your connection to ${ChatColor.BOLD}${event.target.name}${ChatColor.RED} has been blocked due to you not having permission to access it.")
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onKickEvent(event: ServerKickEvent)
    {
        var kickedFrom: ServerInfo?

        if (event.player.server != null)
        {
            kickedFrom = event.player.server.info
        } else if (plugin.proxy.reconnectHandler != null)
        {
            kickedFrom = plugin.proxy.reconnectHandler.getServer(event.player)
        } else
        {
            kickedFrom = AbstractReconnectHandler.getForcedHost(event.player.pendingConnection)

            if (kickedFrom == null)
            {
                kickedFrom = ProxyServer.getInstance()
                    .getServerInfo(event.player.pendingConnection.listener.defaultServer)
            }
        }

        if (kickedFrom != null && plugin.isLobbyServer(kickedFrom))
        {
            return
        }

        val reasonForKick = event.kickReason
        event.isCancelled = true
        event.cancelServer = plugin.fetchBestLobby()

        if (
            reasonForKick.isNotBlank() ||
            !reasonForKick.contains("ban") ||
            !reasonForKick.contains("blacklist")
        )
        {
            event.player.sendMessage(
                *TextComponent.fromLegacyText(
                    """
                ${ChatColor.RED}The server you were on is now down for:
                $reasonForKick
                """.trimIndent()
                )
            )
        }
    }
}
