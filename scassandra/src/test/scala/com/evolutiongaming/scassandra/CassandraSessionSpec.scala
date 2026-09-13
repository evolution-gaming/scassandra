package com.evolutiongaming.scassandra

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import cats.~>
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, PreparedStatement, SimpleStatement}
import com.datastax.oss.driver.api.core.metadata.Metadata as MetadataJ
import com.datastax.oss.driver.api.core.{CqlIdentifier, CqlSession}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.concurrent.CompletableFuture
import java.util.{Map as MapJ, Optional}
import scala.collection.mutable.ListBuffer

class CassandraSessionSpec extends AnyWordSpec with Matchers {

  private val query = "SELECT 1"

  private class SessionStub(
    resultSet: AsyncResultSet = ResultSetMock(),
    prepared: PreparedStatement = ProxyMock[PreparedStatement](PartialFunction.empty),
    keyspace: Option[String] = None,
  ) {
    val calls: ListBuffer[(String, List[AnyRef])] = ListBuffer.empty

    val metadataJ: MetadataJ = ProxyMock[MetadataJ] {
      case ("getClusterName", Nil) => Optional.of("cluster")
    }

    lazy val sessionJ: CqlSession = ProxyMock[CqlSession] {
      case ("getKeyspace", Nil) => keyspace.fold(Optional.empty[CqlIdentifier]) { keyspace =>
          Optional.of(CqlIdentifier.fromInternal(keyspace))
        }
      case ("executeAsync", args) =>
        calls += (("executeAsync", args)); CompletableFuture.completedFuture(resultSet)
      case ("prepareAsync", args) =>
        calls += (("prepareAsync", args)); CompletableFuture.completedFuture(prepared)
      case ("closeAsync", Nil) => calls += (("closeAsync", Nil)); CompletableFuture.completedFuture(null)
      case ("getMetadata", Nil) => metadataJ
    }

    lazy val session: CassandraSession[IO] = CassandraSession[IO](sessionJ)
  }

  "CassandraSession" should {

    "loggedKeyspace" in {
      new SessionStub().session.loggedKeyspace.unsafeRunSync() shouldEqual None
      new SessionStub(keyspace = Some("ks")).session.loggedKeyspace.unsafeRunSync() shouldEqual Some("ks")
    }

    "execute query" in {
      val resultSet = ResultSetMock()
      val stub = new SessionStub(resultSet = resultSet)
      stub.session.execute(query).unsafeRunSync() shouldBe theSameInstanceAs(resultSet)
      stub.calls.toList shouldEqual List(("executeAsync", List(query)))
    }

    "execute query with positional values" in {
      val stub = new SessionStub()
      stub.session.execute("SELECT ?, ?", "a", 1).unsafeRunSync()
      val (name, args) = stub.calls.head
      name shouldEqual "executeAsync"
      args.head shouldEqual "SELECT ?, ?"
      args(1).asInstanceOf[Array[AnyRef]].toList shouldEqual List("a", Int.box(1))
    }

    "execute query with named values" in {
      val stub = new SessionStub()
      stub.session.execute("SELECT :a", Map[String, AnyRef]("a" -> "b")).unsafeRunSync()
      val (name, args) = stub.calls.head
      name shouldEqual "executeAsync"
      args.head shouldEqual "SELECT :a"
      args(1).asInstanceOf[MapJ[String, AnyRef]].get("a") shouldEqual "b"
    }

    "execute statement" in {
      val stub = new SessionStub()
      val statement = SimpleStatement.newInstance(query)
      stub.session.execute(statement).unsafeRunSync()
      stub.calls.head shouldEqual (("executeAsync", List(statement)))
    }

    "prepare query" in {
      val prepared = ProxyMock[PreparedStatement](PartialFunction.empty)
      val stub = new SessionStub(prepared = prepared)
      stub.session.prepare(query).unsafeRunSync() shouldBe theSameInstanceAs(prepared)
      stub.calls.toList shouldEqual List(("prepareAsync", List(query)))
    }

    "prepare statement" in {
      val stub = new SessionStub()
      val statement = SimpleStatement.newInstance(query)
      stub.session.prepare(statement).unsafeRunSync()
      stub.calls.head shouldEqual (("prepareAsync", List(statement)))
    }

    "metadata" in {
      val stub = new SessionStub()
      stub.session.metadata.flatMap(_.clusterName).unsafeRunSync() shouldEqual Some("cluster")
    }

    "of closes the session on release" in {
      val stub = new SessionStub()
      CassandraSession.of[IO](IO(stub.sessionJ)).use(_.execute(query)).unsafeRunSync()
      stub.calls.toList shouldEqual List(("executeAsync", List(query)), ("closeAsync", Nil))
    }

    "mapK" in {
      val stub = new SessionStub()
      val program = for {
        counter <- Ref[IO].of(0)
        counting = new (IO ~> IO) {
          def apply[A](fa: IO[A]): IO[A] = counter.update(_ + 1) *> fa
        }
        session = stub.session.mapK(counting)
        _ <- session.execute("q")
        _ <- session.execute("q", "a")
        _ <- session.execute("q", Map.empty[String, AnyRef])
        _ <- session.execute(SimpleStatement.newInstance("q"))
        _ <- session.prepare("q")
        _ <- session.prepare(SimpleStatement.newInstance("q"))
        _ <- session.loggedKeyspace
        metadata <- session.metadata
        _ <- metadata.clusterName
        count <- counter.get
      } yield count
      program.unsafeRunSync() shouldEqual 9
    }
  }
}
