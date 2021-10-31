package gg.scala.cocoa.player.rank

import gg.scala.cocoa.handler.DataStoreHandler
import java.util.*

class Rank(
    val uuid: UUID = UUID.randomUUID(),
    var name: String
) {
    val children = ArrayList<UUID>()
    val permissions = ArrayList<String>()
}
