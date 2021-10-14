package gg.scala.cocoa.util

import net.evilblock.cubed.Cubed
import java.util.*

/**
 * @author GrowlyX
 * @since 9/12/2021
 */
object UUIDUtil {

    @JvmStatic
    fun formatUUID(id: String): UUID? {
        return UUID.fromString(id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex(), "$1-$2-$3-$4-$5"))
    }

    @JvmStatic
    fun toName(uuid: UUID): String? {
        return Cubed.instance.uuidCache.name(uuid)
    }
}
