package gg.scala.cocoa.util

import gg.scala.cocoa.handler.DataStoreHandler
import java.util.*

/**
 * @author GrowlyX
 * @since 11/1/2021
 */
fun color(uuid: UUID): String
{
    return DataStoreHandler.findRank(
        DataStoreHandler.rankOf[uuid] ?: DataStoreHandler.getDefaultRank().uuid
    )?.color ?: ""
}
