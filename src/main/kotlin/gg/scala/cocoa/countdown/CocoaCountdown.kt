package gg.scala.cocoa.countdown

import co.aikar.commands.ConditionFailedException
import net.md_5.bungee.api.ChatColor
import org.apache.commons.lang3.time.DurationFormatUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author GrowlyX
 * @since 10/23/2021
 */
data class CocoaCountdown(
    val id: String,
    val endDate: Date
)
{
    companion object
    {
        @JvmStatic
        val DATE_FORMAT = SimpleDateFormat("dd/mm/yyyy hh:mm aa")

        @JvmStatic
        fun of(id: String, date: String): CocoaCountdown
        {
            try
            {
                return CocoaCountdown(
                    id, DATE_FORMAT.parse(date)
                )
            } catch (e: Exception)
            {
                throw ConditionFailedException(
                    "Please enter a valid date. (ex. 01/01/2022 14:00 PM)"
                )
            }
        }
    }

    fun fetchMotd(): String
    {
        val formatted = DurationFormatUtils.formatDurationWords(
            endDate.time - System.currentTimeMillis(), true, true
        )

        return if (endDate.before(Date()))
        {
            "${ChatColor.YELLOW}$id ${ChatColor.GRAY}will start in ${ChatColor.GREEN}$formatted"
        } else
        {
            "${ChatColor.YELLOW}$id ${ChatColor.GRAY}has commenced!"
        }
    }
}
