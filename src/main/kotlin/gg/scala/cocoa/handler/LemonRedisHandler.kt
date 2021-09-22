package gg.scala.cocoa.handler

import gg.scala.banana.annotate.Subscribe
import gg.scala.banana.message.Message
import gg.scala.banana.subscribe.marker.BananaHandler
import java.util.*

/**
 * @author GrowlyX
 * @since 9/21/2021
 */
object LemonRedisHandler : BananaHandler {

    @Subscribe("rank-delete")
    fun onRankDelete(message: Message) {
        val rankUuid = UUID.fromString(
            message["uniqueId"]
        )

        DataStoreHandler.ranks.remove(rankUuid)
    }

    @Subscribe("rank-update")
    fun onRankUpdate(message: Message) {
        val completableFuture = DataStoreHandler.rankLayer
            .fetchEntryByKey(message["uniqueId"])

        completableFuture.thenAccept {
            DataStoreHandler.ranks[it.uuid] = it
        }
    }
}
