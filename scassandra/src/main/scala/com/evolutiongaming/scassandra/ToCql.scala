package com.evolutiongaming.scassandra

trait ToCql[-A] { self =>

  def apply(a: A): String
  

  final def imap[B](f: B => A): ToCql[B] = new ToCql[B] {
    def apply(a: B) = self(f(a))
  }
}

object ToCql {

  def apply[A](implicit toCql: ToCql[A]): ToCql[A] = toCql

  def apply[A: ToCql](a: A): String = ToCql[A].apply(a)


  implicit val StrImp: ToCql[String] = new ToCql[String] {
    def apply(a: String) = a
  }


  object Ops {

    implicit class IdOps[A](val self: A) extends AnyVal {
      def toCql(implicit toCql: ToCql[A]): String = ToCql(self)
    }
  }
}