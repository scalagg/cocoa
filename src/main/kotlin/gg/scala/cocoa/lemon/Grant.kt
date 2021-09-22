package gg.scala.cocoa.lemon

import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.player.rank.Rank
import gg.scala.cocoa.util.other.Expirable
import java.util.*

class Grant(
    val uuid: UUID,
    var target: UUID,
    var rankId: UUID,
    var addedBy: UUID?,
    addedAt: Long,
    var addedOn: String,
    var addedReason: String,
    duration: Long
): Expirable(addedAt, duration) {

    var scopes: MutableList<String> = mutableListOf("global")

    var removedReason: String? = null
    var removedOn: String? = null
    var removedBy: UUID? = null
    var removedAt: Long = -1
    var isRemoved = false

    val isActive: Boolean
        get() = !isRemoved && !hasExpired

    fun getRank(): Rank? {
        return DataStoreHandler.findRank(rankId)
    }
}
