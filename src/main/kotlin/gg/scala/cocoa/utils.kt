package gg.scala.cocoa

import gg.scala.banana.message.Message
import net.evilblock.cubed.serializers.Serializers
import java.util.concurrent.ForkJoinPool

/**
 * @author GrowlyX
 * @since 10/13/2021
 */
fun Message.dispatchToCocoa() {
    ForkJoinPool.commonPool().execute {
        CocoaProxyPlugin.instance.proxyBanana.useResource {
            it.publish("cocoa", Serializers.gson.toJson(this))
            it.close()
        }
    }
}

fun Message.dispatchToLemon() {
    ForkJoinPool.commonPool().execute {
        CocoaProxyPlugin.instance.spigotBanana.useResource {
            it.publish("lemon:spigot", Serializers.gson.toJson(this))
            it.close()
        }
    }
}
