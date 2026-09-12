package com.evolutiongaming.scassandra

import java.lang.reflect.{InvocationHandler, Method, Proxy}
import scala.reflect.ClassTag

object ProxyMock {

  def apply[A](
    handler: PartialFunction[(String, List[AnyRef]), AnyRef],
  )(implicit
    tag: ClassTag[A],
  ): A = {
    val clazz = tag.runtimeClass
    val invocationHandler = new InvocationHandler {
      override def invoke(proxy: AnyRef, method: Method, args: Array[AnyRef]): AnyRef = {
        val arguments = Option(args).fold(List.empty[AnyRef])(_.toList)
        handler.applyOrElse(
          (method.getName, arguments),
          (_: (String, List[AnyRef])) =>
            method.getName match {
              case "toString" => s"${ clazz.getSimpleName }Mock"
              case "hashCode" => Int.box(System.identityHashCode(proxy))
              case "equals" => Boolean.box(arguments.head eq proxy)
              case name => sys.error(s"${ clazz.getSimpleName }.$name is not supported")
            },
        )
      }
    }
    Proxy.newProxyInstance(clazz.getClassLoader, Array[Class[?]](clazz), invocationHandler).asInstanceOf[A]
  }
}
