package gg.scala.cocoa.util

import gg.scala.cocoa.lemon.Grant

object GrantRecalculationUtil {

    @JvmStatic
    fun getProminentGrant(grants: List<Grant>): Grant? {
        return grants
            .sortedByDescending { it.addedAt }
            .firstOrNull { it.isActive }
    }
}
