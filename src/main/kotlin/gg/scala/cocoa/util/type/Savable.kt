package gg.scala.cocoa.util.type

import java.util.concurrent.CompletableFuture

interface Savable {

    fun save(): CompletableFuture<Void>

}
