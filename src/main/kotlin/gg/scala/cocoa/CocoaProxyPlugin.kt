package gg.scala.cocoa

import co.aikar.commands.BungeeCommandManager
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.MessageType
import com.imaginarycode.minecraft.redisbungee.RedisBungee
import com.solexgames.datastore.commons.connection.impl.redis.AuthRedisConnection
import com.solexgames.datastore.commons.connection.impl.redis.NoAuthRedisConnection
import com.solexgames.datastore.commons.layer.impl.RedisStorageLayer
import com.solexgames.datastore.commons.storage.impl.RedisStorageBuilder
import gg.scala.banana.Banana
import gg.scala.banana.BananaBuilder
import gg.scala.banana.credentials.BananaCredentials
import gg.scala.banana.options.BananaOptions
import gg.scala.cocoa.command.HubCommand
import gg.scala.cocoa.command.MaintenanceCommand
import gg.scala.cocoa.handler.CocoaRedisHandler
import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.handler.LemonRedisHandler
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.cocoa.listener.PlayerListener
import gg.scala.cocoa.util.FontInformation
import gg.scala.cocoa.util.UUIDUtil
import net.evilblock.cubed.Cubed
import net.evilblock.cubed.util.BungeeConfigUtils
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


/**
 * @author GrowlyX
 * @since 9/12/2021
 */
class CocoaProxyPlugin : Plugin() {

    companion object {
        @JvmStatic
        lateinit var instance: CocoaProxyPlugin
    }

    lateinit var config: Configuration

    lateinit var proxyBanana: Banana
    lateinit var spigotBanana: Banana

    lateinit var maintenanceLayer: RedisStorageLayer<UUID>
    lateinit var settingsLayer: RedisStorageLayer<Boolean>

    val proxySettings = mutableMapOf<String, Boolean>()
    val messages = mutableMapOf<String, String>()

    var maintenanceRank: String? = null

    val maintenance: Boolean
        get() = proxySettings.getOrDefault("maintenance", false)

    override fun onEnable() {
        instance = this

        loadConfig()
        loadRedisHandler()
        loadCommands()
        loadListeners()
    }

    private val CENTER_PX = 132

    fun shouldAllowOrStay(uuid: UUID): Boolean
    {
        val maintenanceRank = maintenanceRank()

        val rankOfPlayer = DataStoreHandler.findRank(
            DataStoreHandler.rankOf[uuid]
        ) ?: DataStoreHandler.getDefaultRank()

        return rankOfPlayer.weight >= maintenanceRank.weight
    }

    fun maintenanceRank(): Rank
    {
        maintenanceRank ?: return DataStoreHandler.getDefaultRank()

        try
        {
            return DataStoreHandler.findRank(
                UUID.fromString(maintenanceRank)
            ) ?: return DataStoreHandler.getDefaultRank()
        } catch (ignored: Exception)
        {
            return DataStoreHandler.getDefaultRank()
        }
    }

    private fun getCenteredMotd(message: String): String
    {
        var messagePxSize = 0
        var previousCode = false
        var isBold = false
        for (c in message.toCharArray())
        {
            if (c == '§')
            {
                previousCode = true
            } else if (previousCode)
            {
                previousCode = false
                isBold = c == 'l' || c == 'L'
            } else
            {
                val dFI = FontInformation.getDefaultFontInfo(c)
                messagePxSize += if (isBold) dFI.getBoldLength() else dFI.getLength()
                messagePxSize++
            }
        }

        val halvedMessageSize = messagePxSize / 2
        val toCompensate = CENTER_PX - halvedMessageSize
        val spaceLength = FontInformation.SPACE.getLength() + 1
        var compensated = 0
        val sb = StringBuilder()

        while (compensated < toCompensate)
        {
            sb.append(" ")
            compensated += spaceLength
        }
        return sb.toString() + message
    }

    private fun loadListeners() {
        proxy.pluginManager.registerListener(this, PlayerListener(this))
    }

    private fun loadCommands() {
        val commandManager = BungeeCommandManager(this)

        listOf<MessageType>(MessageType.HELP, MessageType.INFO, MessageType.SYNTAX).forEach {
            commandManager.getFormat(it).setColor(3, ChatColor.GRAY)
            commandManager.getFormat(it).setColor(2, ChatColor.YELLOW)
            commandManager.getFormat(it).setColor(1, ChatColor.GREEN)
        }

        commandManager.enableUnstableAPI("help")

        commandManager.commandContexts.registerContext(Rank::class.java) {
            val firstArgument = it.popFirstArg()

            return@registerContext DataStoreHandler.findRank(firstArgument)
                ?: throw ConditionFailedException("No rank matching ${ChatColor.YELLOW}$firstArgument${ChatColor.RED} could be found.")
        }

        commandManager.commandCompletions.registerCompletion("ranks") {
            return@registerCompletion DataStoreHandler.ranks.map { it.value.name }
        }

        commandManager.registerCommand(MaintenanceCommand(this))
        commandManager.registerCommand(HubCommand(this))
    }

    fun fetchBestLobby(): ServerInfo? {
        return proxy.servers.values
            .sortedBy { it.players.size }
            .firstOrNull { isLobbyServer(it) }
    }

    fun fetchBestRestrictedLobby(): ServerInfo? {
        return proxy.servers.values
            .firstOrNull { isRestrictedLobbyServer(it) } ?: fetchBestLobby()
    }

    fun isLobbyServer(info: ServerInfo): Boolean {
        return (info.name.contains("hub") || info.name.contains("lobby")) && !info.name.contains("restricted")
    }

    fun isRestrictedLobbyServer(info: ServerInfo): Boolean {
        return (info.name.contains("hub") || info.name.contains("lobby")) && info.name.contains("restricted")
    }

    fun fetchBestMessage(): String {
        return if (maintenance) {
            messages["maintenance-motd"]!!
        } else {
            messages["normal-motd"]!!
        }
    }

    fun addToMaintenanceList(uuid: UUID): CompletableFuture<Void> {
        return maintenanceLayer.saveEntry(uuid.toString(), uuid)
    }

    fun removeFromMaintenanceList(uuid: UUID): CompletableFuture<Void> {
        return maintenanceLayer.deleteEntry(uuid.toString())
    }

    fun updateMaintenanceRank(rank: Rank)
    {
        settingsLayer.runCommand {
            it.hset("cocoa:metadata", "maintenance-rank", rank.uuid.toString())
        }
    }

    fun updateMaintenanceMode(rank: Rank?): CompletableFuture<Void> {
        val newValue = !maintenance
        proxySettings["maintenance"] = newValue

        if (rank != null)
        {
            settingsLayer.runCommand {
                it.hset("cocoa:metadata", "maintenance-rank", rank.uuid.toString())
            }
        }

        return settingsLayer.saveEntry("maintenance", newValue)
    }

    private fun loadConfig() {
        BungeeConfigUtils.saveDefaultConfig(this)

        config = ConfigurationProvider
            .getProvider(YamlConfiguration::class.java)
            .load(File(dataFolder, "config.yml"))

        messages["maintenance-motd"] = getCenteredMotd(ChatColor.translateAlternateColorCodes(
            '&', config.getString("motd.maintenance")
        ))
        messages["normal-motd"] = getCenteredMotd(ChatColor.translateAlternateColorCodes(
            '&', config.getString("motd.normal")
        ))
    }

    private fun loadRedisHandler() {
        val connection = if (!config.getBoolean("redis.authentication")) {
            NoAuthRedisConnection(
                config.getString("redis.address"),
                config.getInt("redis.port")
            )
        } else {
            AuthRedisConnection(
                config.getString("redis.address"),
                config.getInt("redis.port"),
                config.getString("redis.password")
            )
        }
        val credentials = BananaCredentials(
            config.getString("redis.address"),
            config.getInt("redis.port"),
            config.getBoolean("redis.authentication"),
            config.getString("redis.password")
        )

        val executor = Executors.newScheduledThreadPool(1)

        proxyBanana = BananaBuilder()
            .options(
                BananaOptions(
                    channel = "cocoa",
                    async = true
                )
            )
            .credentials(
                credentials
            )
            .build()

        spigotBanana = BananaBuilder()
            .options(
                BananaOptions(
                    channel = "lemon:spigot",
                    async = true,
                    ignoreNonExistentHandlers = true
                )
            )
            .credentials(
                credentials
            )
            .build()

        proxyBanana.registerClass(CocoaRedisHandler)
        spigotBanana.registerClass(LemonRedisHandler)

        proxyBanana.subscribe()
        spigotBanana.subscribe()

        maintenanceLayer = RedisStorageBuilder<UUID>()
            .setConnection(connection)
            .setType(UUID::class.java)
            .setSection("cocoa:maintenance")
            .build()

        settingsLayer = RedisStorageBuilder<Boolean>()
            .setConnection(connection)
            .setType(Boolean::class.java)
            .setSection("cocoa:settings")
            .build()

        Cubed.instance.loadStore(proxyBanana)

        DataStoreHandler.initialLoad()

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            proxy.players.forEach {
                DataStoreHandler.updateRank(it.uniqueId)
            }
        }, 0L, 2L, TimeUnit.SECONDS)

        executor.scheduleAtFixedRate({
            settingsLayer.fetchAllEntries().thenAccept {
                proxySettings.clear()

                it.forEach { entry ->
                    proxySettings[entry.key] = entry.value
                }
            }

            try {
                settingsLayer.runCommand {
                    it.hset("cocoa:players", "global", RedisBungee.getApi().playerCount.toString())

                    maintenanceRank = it.hget("cocoa:metadata", "maintenance-rank")
                }
            } catch (e: Exception) {
                logger.warning("Failed to update online players! Is RedisBungee not enabled?")
            }
        }, 0L, 1L, TimeUnit.SECONDS)
    }
}
