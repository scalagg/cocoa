package gg.scala.cocoa.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.annotation.*
import gg.scala.cocoa.CocoaProxyPlugin
import gg.scala.cocoa.CocoaUtilities
import gg.scala.cocoa.handler.CocoaRedisHandler
import gg.scala.cocoa.util.UUIDUtil
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import java.util.*

/**
 * @author GrowlyX
 * @since 9/12/2021
 */
@CommandAlias("maintenance")
@CommandPermission("cocoa.command.maintenance")
class MaintenanceCommand(private val plugin: CocoaProxyPlugin) : BaseCommand() {

    @Default
    @HelpCommand
    @Syntax("[help]")
    fun onHelp(help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("toggle")
    @Description("Toggle maintenance mode.")
    fun onToggle(sender: CommandSender) {
        plugin.updateMaintenanceMode().thenRun {
            val currentStatus = plugin.maintenance

            sender.sendMessage(
                if (currentStatus) {
                    CocoaUtilities.dispatchToCocoa(
                        CocoaRedisHandler.buildMessage(
                            "maintenance-kick",
                            mapOf()
                        )
                    )

                    "${ChatColor.GREEN}Maintenance mode has been enabled."
                } else {
                    "${ChatColor.RED}Maintenance mode has been disabled."
                }
            )
        }
    }

    @Subcommand("add")
    @Syntax("[player]")
    @Description("Add a player to maintenance.")
    fun onAdd(sender: CommandSender, uuid: UUID) {
        if (plugin.whitelistedPlayers.contains(uuid)) {
            throw ConditionFailedException("That player is already whitelisted.")
        }

        plugin.addToMaintenanceList(uuid).thenRun {
            sender.sendMessage("${ChatColor.YELLOW}You've whitelisted ${ChatColor.GREEN}${UUIDUtil.toName(uuid)}${ChatColor.YELLOW} globally.")
        }
    }

    @Subcommand("remove")
    @Syntax("[player]")
    @Description("Remove a player from maintenance.")
    fun onRemove(sender: CommandSender, uuid: UUID) {
        if (!plugin.whitelistedPlayers.contains(uuid)) {
            throw ConditionFailedException("That player is not whitelisted.")
        }

        plugin.removeFromMaintenanceList(uuid).thenRun {
            sender.sendMessage("${ChatColor.YELLOW}You've un-whitelisted ${ChatColor.GREEN}${UUIDUtil.toName(uuid)}${ChatColor.YELLOW} globally.")
        }
    }

    @Subcommand("list|view")
    @Description("View all whitelisted players.")
    fun onList(sender: CommandSender) {
        if (plugin.whitelistedPlayers.isEmpty()) {
            throw ConditionFailedException("No one is whitelisted.")
        }

        sender.sendMessage("${ChatColor.YELLOW}Fetching maintenance list...")

        plugin.maintenanceLayer.fetchAllEntries().thenApply {
            val nameList = mutableListOf<String>()

            it.forEach { entry ->
                UUIDUtil.toName(entry.value)?.let { it1 -> nameList.add(it1) }
            }

            return@thenApply nameList
        }.thenAccept {
            sender.sendMessages(
                "${ChatColor.GREEN}${ChatColor.BOLD}Whitelisted Players:",
                "${ChatColor.YELLOW}${it.size}${ChatColor.GRAY} whitelisted players found.",
                ""
            )

            it.forEach { name ->
                sender.sendMessage("${ChatColor.GRAY} - ${ChatColor.WHITE}$name")
            }
        }
    }

}
