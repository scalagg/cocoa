package gg.scala.cocoa.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.annotation.CommandAlias
import gg.scala.cocoa.CocoaProxyPlugin
import net.md_5.bungee.api.connection.ProxiedPlayer

/**
 * @author GrowlyX
 * @since 9/12/2021
 */
class HubCommand(
    private val plugin: CocoaProxyPlugin
) : BaseCommand()
{

    @CommandAlias("hub|lobby")
    fun onHub(player: ProxiedPlayer)
    {
        if (plugin.isLobbyServer(player.server.info))
        {
            throw ConditionFailedException("You're already in a hub.")
        }

        val bestLobby = if (player.hasPermission("cocoa.staff"))
        {
            plugin.fetchBestRestrictedLobby() ?: plugin.fetchBestLobby()
        } else plugin.fetchBestLobby() ?: throw ConditionFailedException("We couldn't find an available lobby for you.")

        player.connect(bestLobby)
    }

}
