package com.solexgames.cocoa

import net.md_5.bungee.api.plugin.Plugin

/**
 * @author GrowlyX
 * @since 9/12/2021
 */
class CocoaProxyPlugin : Plugin() {

    companion object {
        @JvmStatic
        lateinit var instance: CocoaProxyPlugin
    }

    override fun onEnable() {
        instance = this


    }
}
