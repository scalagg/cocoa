package gg.scala.cocoa

import gg.scala.banana.message.Message
import net.evilblock.cubed.serializers.Serializers
import java.util.concurrent.ForkJoinPool

/**
 * @author GrowlyX
 * @since 10/13/2021
 */
object CocoaUtilities
{

    @JvmStatic
    fun dispatchToCocoa(message: Message) {
        ForkJoinPool.commonPool().execute {
            CocoaProxyPlugin.instance.proxyBanana.useResource {
                it.publish("cocoa", Serializers.gson.toJson(message))
                it.close()
            }
        }
    }

    @JvmStatic
    fun dispatchToLemon(message: Message) {
        ForkJoinPool.commonPool().execute {
            CocoaProxyPlugin.instance.spigotBanana.useResource {
                it.publish("lemon:spigot", Serializers.gson.toJson(message))
                it.close()
            }
        }
    }

}
