package com.evolutiongaming.scassandra

import cats.effect.IO
import cats.syntax.all.*
import com.datastax.driver.core.{Row, SimpleStatement, Statement}
import com.evolutiongaming.catshelper.CatsHelper.*
import com.evolutiongaming.scassandra.StreamingCassandraSession.*
import com.evolutiongaming.scassandra.syntax.*
import com.evolutiongaming.sstream.Stream.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class PagingSpec extends AnyFunSuite with CassandraSuite with Matchers {

  import PagingSpec.*

  private lazy val table = s"$keyspace.paging"

  private lazy val query = s"SELECT id FROM $table"

  private def paged: Statement = new SimpleStatement(query).setFetchSize(FetchSize)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val program = for {
      _ <- session.execute(s"CREATE TABLE IF NOT EXISTS $table (id INT PRIMARY KEY)")
      prepared <- session.prepare(s"INSERT INTO $table (id) VALUES (?)")
      _ <- Ids.traverse_ { id => session.execute(prepared.bind().encode("id", id)) }
    } yield ()
    program.toTry.get
  }

  private def ids(rows: List[Row]): List[Int] = rows.map(_.decode[Int]("id")).sorted

  test("statement fetch size splits the result into pages") {
    val resultSet = session.execute(paged).toTry.get
    resultSet.isFullyFetched shouldEqual false
    resultSet.getAvailableWithoutFetching shouldEqual FetchSize
  }

  test("default fetch size returns everything in one page") {
    val resultSet = session.execute(query).toTry.get
    resultSet.isFullyFetched shouldEqual true
    resultSet.getAvailableWithoutFetching shouldEqual Ids.size
  }

  test("ResultSet.stream reads all pages") {
    val rows = session.execute(paged).flatMap(_.stream[IO].toList).toTry.get
    ids(rows) shouldEqual Ids
  }

  test("executeStream reads all pages") {
    ids(session.executeStream(paged).toList.toTry.get) shouldEqual Ids
    ids(session.executeStream(query).toList.toTry.get) shouldEqual Ids
  }

  test("stream stops as soon as the fold completes") {
    val stream = session.executeStream(paged)
    stream.first.toTry.get.map(_ => ()) shouldEqual Some(())
    val taken = stream.foldWhileM(List.empty[Int]) { (seen, row) =>
      val seen1 = row.decode[Int]("id") :: seen
      IO.pure(if (seen1.size == FetchSize + 1) Right(seen1) else Left(seen1))
    }
    taken.toTry.get.map(_.size) shouldEqual Right(FetchSize + 1)
  }

  test("QueryConfig.fetchSize applies to every query of the cluster") {
    val config1 = config.copy(query = QueryConfig(fetchSize = FetchSize))
    val resource = for {
      clusterOf <- CassandraClusterOf.of[IO].toResource
      cluster <- clusterOf(config1)
      session <- cluster.connect
    } yield session
    val program = resource.use { session =>
      for {
        resultSet <- session.execute(query)
        rows <- resultSet.stream[IO].toList
      } yield (resultSet.getAvailableWithoutFetching, ids(rows))
    }
    val (available, rows) = program.toTry.get
    available shouldEqual 0
    rows shouldEqual Ids
  }
}

object PagingSpec {

  val FetchSize = 4

  val Ids: List[Int] = (0 until 25).toList
}
