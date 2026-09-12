package com.evolutiongaming.scassandra

import cats.arrow.FunctionK
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import cats.~>
import com.datastax.driver.core.{
  CloseFutureMock,
  Host,
  PreparedStatement,
  ResultSet,
  Session as SessionJ,
  SimpleStatement,
}
import com.google.common.util.concurrent.Futures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.{Collections, Map as MapJ}
import scala.annotation.nowarn
import scala.collection.mutable.ListBuffer

class CassandraSessionSpec extends AnyWordSpec with Matchers {

  private def stateJ(hosts: Int, open: Int, trashed: Int, inFlight: Int): SessionJ.State = ProxyMock[SessionJ.State] {
    case ("getConnectedHosts", Nil) => Collections.nCopies(hosts, null: Host)
    case ("getOpenConnections", _) => Int.box(open)
    case ("getTrashedConnections", _) => Int.box(trashed)
    case ("getInFlightQueries", _) => Int.box(inFlight)
  }

  private class SessionStub(
    resultSet: ResultSet = ResultSetMock(),
    prepared: PreparedStatement = ProxyMock[PreparedStatement](PartialFunction.empty),
    states: List[SessionJ.State] = List(stateJ(1, 1, 0, 0)),
    keyspace: String = null,
  ) {
    val calls: ListBuffer[(String, List[AnyRef])] = ListBuffer.empty
    private val statesLeft = Iterator.continually(states).flatten

    lazy val sessionJ: SessionJ = ProxyMock[SessionJ] {
      case ("getLoggedKeyspace", Nil) => keyspace
      case ("initAsync", Nil) => calls += (("initAsync", Nil)); Futures.immediateFuture(sessionJ)
      case ("executeAsync", args) => calls += (("executeAsync", args)); ResultSetFutureMock(resultSet)
      case ("prepareAsync", args) => calls += (("prepareAsync", args)); Futures.immediateFuture(prepared)
      case ("closeAsync", Nil) => calls += (("closeAsync", Nil)); CloseFutureMock()
      case ("getState", Nil) => statesLeft.next()
    }

    lazy val session: CassandraSession[IO] = CassandraSession[IO](sessionJ)
  }

  "CassandraSession" should {

    "loggedKeyspace" in {
      new SessionStub().session.loggedKeyspace.unsafeRunSync() shouldEqual None
      new SessionStub(keyspace = "ks").session.loggedKeyspace.unsafeRunSync() shouldEqual Some("ks")
    }

    "init" in {
      val stub = new SessionStub()
      stub.session.init.unsafeRunSync()
      stub.calls.toList shouldEqual List(("initAsync", Nil))
    }

    "execute query" in {
      val resultSet = ResultSetMock()
      val stub = new SessionStub(resultSet = resultSet)
      stub.session.execute("SELECT 1").unsafeRunSync() shouldBe theSameInstanceAs(resultSet)
      stub.calls.toList shouldEqual List(("executeAsync", List("SELECT 1")))
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
      val statement = new SimpleStatement("SELECT 1")
      stub.session.execute(statement).unsafeRunSync()
      stub.calls.head shouldEqual (("executeAsync", List(statement)))
    }

    "prepare query" in {
      val prepared = ProxyMock[PreparedStatement](PartialFunction.empty)
      val stub = new SessionStub(prepared = prepared)
      stub.session.prepare("SELECT 1").unsafeRunSync() shouldBe theSameInstanceAs(prepared)
      stub.calls.toList shouldEqual List(("prepareAsync", List("SELECT 1")))
    }

    "prepare statement" in {
      val stub = new SessionStub()
      val statement = new SimpleStatement("SELECT 1")
      stub.session.prepare(statement).unsafeRunSync()
      stub.calls.head shouldEqual (("prepareAsync", List(statement)))
    }

    "stateSnapshot" in {
      val stub = new SessionStub(states = List(stateJ(2, 3, 4, 5)))
      val snapshot = stub.session.stateSnapshot.unsafeRunSync()
      snapshot.connectedHosts.size shouldEqual 2
      snapshot.openConnections(null) shouldEqual 3
      snapshot.trashedConnections(null) shouldEqual 4
      snapshot.inFlightQueries(null) shouldEqual 5
    }

    "state takes a fresh snapshot on every access" in {
      val stub = new SessionStub(states = List(stateJ(0, 0, 0, 0), stateJ(2, 1, 0, 0)))
      val state = stub.session.state: @nowarn("cat=deprecation")
      state.connectedHosts.unsafeRunSync().size shouldEqual 0
      state.connectedHosts.unsafeRunSync().size shouldEqual 2
    }

    "of closes the session on release" in {
      val stub = new SessionStub()
      CassandraSession.of[IO](IO(stub.sessionJ)).use(_.init).unsafeRunSync()
      stub.calls.toList shouldEqual List(("initAsync", Nil), ("closeAsync", Nil))
    }

    "mapK" in {
      val stub = new SessionStub()
      val program = for {
        counter <- Ref[IO].of(0)
        counting = new (IO ~> IO) {
          def apply[A](fa: IO[A]): IO[A] = counter.update(_ + 1) *> fa
        }
        session = stub.session.mapK(counting)
        _ <- session.init
        _ <- session.execute("q")
        _ <- session.execute("q", "a")
        _ <- session.execute("q", Map.empty[String, AnyRef])
        _ <- session.execute(new SimpleStatement("q"))
        _ <- session.prepare("q")
        _ <- session.prepare(new SimpleStatement("q"))
        _ <- session.loggedKeyspace
        _ <- session.stateSnapshot
        _ <- (session.state: @nowarn("cat=deprecation")).connectedHosts
        count <- counter.get
      } yield count
      program.unsafeRunSync() shouldEqual 10
    }
  }

  "CassandraSession.StateSnapshot" should {

    "wrap the driver state" in {
      val snapshot = CassandraSession.StateSnapshot.wrapImmutable(stateJ(1, 2, 3, 4))
      snapshot.connectedHosts.size shouldEqual 1
      snapshot.openConnections(null) shouldEqual 2
      snapshot.trashedConnections(null) shouldEqual 3
      snapshot.inFlightQueries(null) shouldEqual 4
    }
  }

  "CassandraSession.State" should {

    "wrap the driver state" in {
      val state = (CassandraSession.State[IO](stateJ(1, 2, 3, 4)): @nowarn("cat=deprecation"))
      state.connectedHosts.unsafeRunSync().size shouldEqual 1
      state.openConnections(null).unsafeRunSync() shouldEqual 2
      state.trashedConnections(null).unsafeRunSync() shouldEqual 3
      state.inFlightQueries(null).unsafeRunSync() shouldEqual 4
    }

    "mapK" in {
      val state = (CassandraSession.State[IO](stateJ(1, 2, 3, 4)): @nowarn("cat=deprecation")).mapK(FunctionK.id[IO])
      state.connectedHosts.unsafeRunSync().size shouldEqual 1
      state.openConnections(null).unsafeRunSync() shouldEqual 2
      state.trashedConnections(null).unsafeRunSync() shouldEqual 3
      state.inFlightQueries(null).unsafeRunSync() shouldEqual 4
    }
  }
}
