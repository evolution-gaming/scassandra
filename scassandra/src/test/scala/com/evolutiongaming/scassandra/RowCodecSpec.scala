package com.evolutiongaming.scassandra

import cats.{Contravariant, Functor}
import com.datastax.driver.core.{GettableByNameData, SettableData, SimpleStatement}
import com.evolutiongaming.scassandra.syntax.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RowCodecSpec extends AnyWordSpec with Matchers {

  import RowCodecSpec.*

  "EncodeRow" should {

    "encode a value under a fixed name" in {
      EncodeRow[String]("key").apply(DataMock(), "value").byName shouldEqual Map("key" -> "value")
    }

    "contramap" in {
      EncodeRow[String]("key").contramap[Int](_.toString).apply(DataMock(), 1).byName shouldEqual Map("key" -> "1")
    }

    "have a Contravariant instance" in {
      val encode = Contravariant[EncodeRow].contramap(EncodeRow[String]("key"))((_: Int).toString)
      encode(DataMock(), 2).byName shouldEqual Map("key" -> "2")
    }

    "be summoned and used via Ops" in {
      EncodeRow[User].apply(DataMock(), User("name", 1)).byName shouldEqual user
      new EncodeRow.Ops.SettableDataOps(DataMock()).encode(User("name", 1)).byName shouldEqual user
    }
  }

  "DecodeRow" should {

    "decode a value from a fixed name" in {
      DecodeRow[String]("key").apply(DataMock(byName = Map("key" -> "value"))) shouldEqual "value"
    }

    "map" in {
      DecodeRow[String]("key").map(_.length).apply(DataMock(byName = Map("key" -> "value"))) shouldEqual 5
    }

    "have a Functor instance" in {
      val decode = Functor[DecodeRow].map(DecodeRow[String]("key"))(_.toUpperCase)
      decode(DataMock(byName = Map("key" -> "value"))) shouldEqual "VALUE"
    }

    "be summoned and used via Ops" in {
      val data = DataMock(byName = user)
      DecodeRow[User].apply(data) shouldEqual User("name", 1)
      new DecodeRow.Ops.GettableByNameDataOps(data).decode[User] shouldEqual User("name", 1)
    }
  }

  "UpdateRow" should {

    "be derived from EncodeRow" in {
      UpdateRow[User].apply(DataMock(), User("name", 1)).byName shouldEqual user
    }

    "contramap" in {
      val update = UpdateRow[User].contramap[String](User(_, 0))
      update(DataMock(), "name").byName shouldEqual Map[String, Any]("name" -> "name", "age" -> 0)
    }
  }

  "UpdateByName" should {

    "contramap" in {
      val update = UpdateByName[String].contramap[Int](_.toString)
      update(DataMock(), "key", 1).byName shouldEqual Map("key" -> "1")
    }
  }

  "UpdateByIdx" should {

    "contramap" in {
      val update = UpdateByIdx[String].contramap[Int](_.toString)
      update(DataMock(), 0, 1).byIdx shouldEqual Map(0 -> "1")
    }
  }

  "syntax" should {

    "encode a row" in {
      DataMock().encode(User("name", 1)).byName shouldEqual user
    }

    "encodeSome by name" in {
      DataMock().encodeSome("key", Some("value")).byName shouldEqual Map("key" -> "value")
      DataMock().encodeSome("key", None: Option[String]).byName shouldEqual Map.empty
    }

    "encodeSome a row" in {
      DataMock().encodeSome(Some(User("name", 1))).byName shouldEqual user
      DataMock().encodeSome(None: Option[User]).byName shouldEqual Map.empty
    }

    "decode a row" in {
      DataMock(byName = user).decode[User] shouldEqual User("name", 1)
    }

    "update a row" in {
      DataMock().update(User("name", 1)).byName shouldEqual user
    }

    "toggle statement tracing" in {
      new SimpleStatement("SELECT 1").trace(enable = true).isTracing shouldEqual true
      new SimpleStatement("SELECT 1").trace(enable = false).isTracing shouldEqual false
    }
  }
}

object RowCodecSpec {

  final case class User(name: String, age: Int)

  val user: Map[String, Any] = Map("name" -> "name", "age" -> 1)

  implicit val encodeRowUser: EncodeRow[User] = new EncodeRow[User] {
    def apply[B <: SettableData[B]](data: B, user: User): B = {
      data
        .encode("name", user.name)
        .encode("age", user.age)
    }
  }

  implicit val decodeRowUser: DecodeRow[User] = (data: GettableByNameData) => {
    User(data.decode[String]("name"), data.decode[Int]("age"))
  }
}
