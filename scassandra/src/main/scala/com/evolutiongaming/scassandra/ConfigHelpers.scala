package com.evolutiongaming.scassandra

import com.evolutiongaming.config.ConfigHelper.FromConf
import com.evolutiongaming.nel.Nel
import com.typesafe.config.ConfigException
import pureconfig.ConfigReader
import pureconfig.module.cats.EmptyTraversableFound

import scala.reflect.ClassTag

object ConfigHelpers {

  implicit def nelFromConf[A](implicit fromConf: FromConf[List[A]]): FromConf[Nel[A]] = {
    FromConf { case (conf, path) =>
      val list = fromConf(conf, path)
      list match {
        case Nil     => throw new ConfigException.BadValue(conf.origin(), path, s"Cannot parse Nel from empty list")
        case x :: xs => Nel(x, xs)
      }
    }
  }

  implicit def nelReader[T](implicit reader: ConfigReader[List[T]]): ConfigReader[Nel[T]] = {
    reader.emap { x =>
      Nel.opt(x).toRight(EmptyTraversableFound(implicitly[ClassTag[Nel[T]]].toString()))
    }
  }
}
