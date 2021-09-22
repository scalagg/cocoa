package gg.scala.cocoa.util.type

import gg.scala.cocoa.util.type.Loadable
import gg.scala.cocoa.util.type.Savable

interface Persistent<T>: Loadable<T>, Savable
