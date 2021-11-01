package gg.scala.cocoa.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.annotation.*
import gg.scala.cocoa.CocoaProxyPlugin
import gg.scala.cocoa.handler.CocoaRedisHandler
import gg.scala.cocoa.lemon.rank.Rank
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender

/**
 * @author GrowlyX
 * @since 9/12/2021
 */
@CommandAlias("maintenance")
@CommandPermission("cocoa.command.maintenance")
class MaintenanceCommand(private val plugin: CocoaProxyPlugin) : BaseCommand()
{

    @Default
    @HelpCommand
    @Syntax("[help]")
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
    }

    @Subcommand("update")
    @Description("Update maintenance mode's rank.")
    @CommandCompletion("@ranks")
    fun onUpdateRank(sender: CommandSender, rank: Rank)
    {
        val currentStatus = plugin.maintenance

        if (!currentStatus)
        {
            throw ConditionFailedException("You must enable maintenance mode to perform this command.")
        }

        plugin.updateMaintenanceRank(rank)

        sender.sendMessage("${ChatColor.GREEN}Maintenance mode rank has been set to: ${rank.color + rank.name}")
    }

    @Subcommand("toggle")
    @Description("Toggle maintenance mode.")
    @CommandCompletion("@ranks")
    fun onToggle(sender: CommandSender, rank: Rank?)
    {
        val currentStatus = plugin.maintenance

        if (!currentStatus && rank == null)
        {
            throw ConditionFailedException("You must define a rank to toggle maintenance mode")
        }

        plugin.updateMaintenanceMode(rank).thenRun {
            sender.sendMessage(
                if (!currentStatus)
                {
                    CocoaRedisHandler.buildMessage(
                        "maintenance-kick",
                        mapOf()
                    ).dispatch(
                        CocoaProxyPlugin.instance.proxyBanana,
                    )

                    "${ChatColor.GREEN}Maintenance mode has been enabled."
                } else
                {
                    "${ChatColor.RED}Maintenance mode has been disabled."
                }
            )
        }
    }


    @Subcommand("info")
    @Description("View maintenance information.")
    fun onList(sender: CommandSender)
    {
        if (plugin.maintenance)
        {
            val maintenanceRank = plugin.maintenanceRank()

            sender.sendMessage(
                "${ChatColor.GREEN}The server is currently restricted to players with a rank weight above: ${maintenanceRank.color + maintenanceRank.name}"
            )
        } else
        {
            sender.sendMessage("${ChatColor.RED}The server is not in maintenance mode.")
        }
    }

}
