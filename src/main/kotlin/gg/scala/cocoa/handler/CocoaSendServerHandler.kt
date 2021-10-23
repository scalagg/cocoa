package gg.scala.cocoa.handler

import gg.scala.banana.annotate.Subscribe
import gg.scala.banana.message.Message
import net.md_5.bungee.api.ProxyServer
import java.util.*

/**
 * @author GrowlyX
 * @since 10/23/2021
 */
class CocoaSendServerHandler
{

    @Subscribe("send-server")
    fun onSendServer(message: Message)
    {
        val uniqueId = UUID.fromString(message["uuid"])
        val proxied = ProxyServer.getInstance().getPlayer(uniqueId)

        if (proxied != null)
        {
            val info = ProxyServer.getInstance().getServerInfo(
                message["server"]
            )

            if (info != null)
            {
                if (proxied.server.info == info)
                {
                    return
                }

                proxied.connect(info)

                println("Connecting now")
            }
        }
    }
}
