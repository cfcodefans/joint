package org.joint.scala

/**
  * Created by fan on 2017/1/30.
  */
object ScalaMiscs {
    type ReflectiveCloseable = {
        def close()
    }
    @inline def try_[A <: ReflectiveCloseable, B](closeAble: A)(f: (A) => B): B = {
        try {
            f(closeAble)
        } finally {
            if (closeAble != null)
                closeAble.close()
        }
    }
}
