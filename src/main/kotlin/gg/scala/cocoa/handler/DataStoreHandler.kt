package gg.scala.cocoa.handler

import com.mongodb.client.model.Filters
import com.solexgames.datastore.commons.connection.impl.mongo.UriMongoConnection
import com.solexgames.datastore.commons.layer.impl.MongoStorageLayer
import com.solexgames.datastore.commons.storage.impl.MongoStorageBuilder
import gg.scala.cocoa.lemon.Grant
import gg.scala.cocoa.lemon.rank.Rank
import net.evilblock.cubed.CubedConfig
import net.evilblock.cubed.serializers.Serializers
import org.bson.conversions.Bson
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 9/21/2021
 */
object DataStoreHandler {

    val ranks = mutableMapOf<UUID, Rank>()

    lateinit var rankLayer: MongoStorageLayer<Rank>
    lateinit var grantLayer: MongoStorageLayer<Grant>

    val rankOf = mutableMapOf<UUID, UUID>()

    fun initialLoad()
    {
        val mongoConnection = UriMongoConnection(
            CubedConfig.getMongoURI()
        )
        val database = "SolexGames"

        this.rankLayer = MongoStorageBuilder<Rank>()
            .setDatabase(database).setCollection("lemon_ranks")
            .setConnection(mongoConnection).setType(Rank::class.java).build()

        this.grantLayer = MongoStorageBuilder<Grant>()
            .setDatabase(database).setCollection("lemon_grants")
            .setConnection(mongoConnection).setType(Grant::class.java).build()

        rankLayer.fetchAllEntries().whenComplete { entries, _ ->
            entries.forEach {
                ranks[it.value.uuid] = it.value
            }
        }
    }

    fun updateRank(uuid: UUID)
    {
        fetchGrantsFor(uuid).thenAccept {
            if (it.isNullOrEmpty())
            {
                rankOf[uuid] = getDefaultRank().uuid
            } else
            {
                val prominent = getProminentGrant(it)

                if (prominent != null)
                {
                    rankOf[uuid] = prominent.getRank()?.uuid ?: getDefaultRank().uuid
                } else
                {
                    rankOf[uuid] = getDefaultRank().uuid
                }
            }
        }
    }

    fun loadRankOfNow(uuid: UUID): Rank
    {
        val grants = grantLayer.fetchAllEntriesWithFilterSync(
            Filters.eq("target", uuid.toString())
        ) ?: return getDefaultRank()

        val prominent = getProminentGrant(grants.values)

        return if (prominent == null)
        {
            getDefaultRank()
        } else
        {
            prominent.getRank() ?: getDefaultRank()
        }
    }

    fun findRank(uuid: UUID?): Rank? {
        return ranks.values.firstOrNull {
            it.uuid == uuid
        }
    }

    @JvmStatic
    fun getProminentGrant(grants: Collection<Grant>): Grant? {
        return grants
            .sortedByDescending { it.addedAt }
            .sortedByDescending { it.getRank()?.weight ?: 0 }
            .firstOrNull { !it.isRemoved && !it.hasExpired && it.getRank()?.visible ?: false && it.scopes.contains("global") }
    }

    fun findRank(name: String): Rank? {
        return ranks.values.firstOrNull {
            it.name.equals(name, true)
        }
    }

    private fun fetchGrants(filter: Bson, test: (Grant) -> Boolean): CompletableFuture<List<Grant>> {
        return grantLayer.fetchAllEntriesWithFilter(filter).thenApply {
            val mutableList = mutableListOf<Grant>()

            it.forEach { entry ->
                if (test.invoke(entry.value)) {
                    mutableList.add(entry.value)
                }
            }

            return@thenApply mutableList
        }
    }

    fun fetchGrantsFor(uuid: UUID?): CompletableFuture<List<Grant>> {
        return fetchGrants(
            Filters.eq("target", uuid.toString())
        ) { true }
    }

    fun getDefaultRank(): Rank {
        return findRank("Default")!!
    }
}
