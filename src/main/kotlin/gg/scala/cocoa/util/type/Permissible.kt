package gg.scala.cocoa.util.type

interface Permissible<T> {

    fun getPermission(): String?

    fun hasPermission(t: T): Boolean

}
