package gg.scala.cocoa.util.other

import java.util.*

/**
 * @author puugz, GrowlyX
 * @since 23/08/2021 18:32
 */
open class Expirable(
    val addedAt: Long,
    val duration: Long
) {
    var expireDate: Date = Date(addedAt + duration)

    val isPermanent: Boolean
        get() = duration == Long.MAX_VALUE

    val hasExpired: Boolean
        get() = !isPermanent && System.currentTimeMillis() >= addedAt + duration
}
