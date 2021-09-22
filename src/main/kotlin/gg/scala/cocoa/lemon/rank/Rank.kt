package gg.scala.cocoa.player.rank

import gg.scala.cocoa.handler.DataStoreHandler
import java.util.*

class Rank(
    val uuid: UUID = UUID.randomUUID(),
    var name: String
) {

    val children = ArrayList<UUID>()
    val permissions = ArrayList<String>()

    /**
     * Returns a list with permissions from
     * all inherited ranks as well as the current rank
     */
    fun getCompoundedPermissions(): ArrayList<String> {
        val compoundedPermissions = ArrayList<String>()

        permissions.forEach {
            if (!compoundedPermissions.contains(it)) {
                compoundedPermissions.add(it)
            }
        }

        children.forEach {
            DataStoreHandler.findRank(it)?.let { rank ->
                rank.getCompoundedPermissions().forEach { permission ->
                    if (!compoundedPermissions.contains(permission)) {
                        compoundedPermissions.add(permission)
                    }
                }
            }
        }

        return compoundedPermissions
    }
}
