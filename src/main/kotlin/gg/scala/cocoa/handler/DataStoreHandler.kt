package gg.scala.cocoa.handler

import com.mongodb.client.model.Filters
import com.solexgames.datastore.commons.connection.impl.mongo.UriMongoConnection
import com.solexgames.datastore.commons.layer.impl.MongoStorageLayer
import com.solexgames.datastore.commons.storage.impl.MongoStorageBuilder
import gg.scala.cocoa.CocoaProxyConstants
import gg.scala.cocoa.CocoaProxyPlugin
import gg.scala.cocoa.lemon.Grant
import gg.scala.cocoa.player.rank.Rank
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

    var rankLayer: MongoStorageLayer<Rank>
    var grantLayer: MongoStorageLayer<Grant>

    init {
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

    fun findRank(uuid: UUID): Rank? {
        return ranks.values.firstOrNull {
            it.uuid == uuid
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
            Filters.eq("target", uuid)
        ) { true }
    }

}
