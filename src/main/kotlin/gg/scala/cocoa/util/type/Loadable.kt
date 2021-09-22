package gg.scala.cocoa.util.type

import java.util.concurrent.CompletableFuture

interface Loadable<T> {

    fun load(future: CompletableFuture<T>)

}
