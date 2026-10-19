package com.evolution.scassandra

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import com.datastax.oss.driver.api.core.cql.{Row, SimpleStatement}
import com.evolution.scassandra.PagingSpec.*
import com.evolution.scassandra.syntax.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class PagingSpec extends AnyFunSuite with CassandraSuite with Matchers {

  private lazy val table = s"$keyspace.paging"

  private lazy val query = s"SELECT id FROM $table"

  private def paged: SimpleStatement = SimpleStatement.newInstance(query).setPageSize(PageSize)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val program = for {
      _ <- session.execute(s"CREATE TABLE IF NOT EXISTS $table (id INT PRIMARY KEY)")
      prepared <- session.prepare(s"INSERT INTO $table (id) VALUES (?)")
      _ <- Ids.traverse_ { id => session.execute(prepared.bind().encode("id", id)) }
    } yield ()
    program.unsafeRunSync()
  }

  private def ids(rows: List[Row]): List[Int] = rows.map(_.decode[Int]("id")).sorted

  test("statement page size splits the result into pages") {
    val resultSet = session.execute(paged).unsafeRunSync()
    resultSet.hasMorePages shouldEqual true
    resultSet.remaining() shouldEqual PageSize
  }

  test("default page size returns everything in one page") {
    val resultSet = session.execute(query).unsafeRunSync()
    resultSet.hasMorePages shouldEqual false
    resultSet.remaining() shouldEqual Ids.size
  }

  test("AsyncResultSet.stream reads all pages") {
    val rows = session.execute(paged).flatMap(_.stream[IO].toList).unsafeRunSync()
    ids(rows) shouldEqual Ids
  }

  test("executeStream reads all pages") {
    ids(session.executeStream(paged).toList.unsafeRunSync()) shouldEqual Ids
    ids(session.executeStream(query).toList.unsafeRunSync()) shouldEqual Ids
  }

  test("fold stopped in the middle of a page returns its result, also across pages") {
    val stream = session.executeStream(paged)
    stream.first.unsafeRunSync().map(_ => ()) shouldEqual Some(())
    val taken = stream.foldWhileM(List.empty[Int]) { (seen, row) =>
      val seen1 = row.decode[Int]("id") :: seen
      IO.pure(if (seen1.size == PageSize + 1) Right(seen1) else Left(seen1))
    }
    taken.unsafeRunSync().map(_.size) shouldEqual Right(PageSize + 1)
  }

  test("basic.request.page-size applies to every query of the session") {
    withSession(s"basic.request.page-size = $PageSize") { session =>
      for {
        resultSet <- session.execute(query)
        // checked before streaming, as the stream consumes the rows
        _ = resultSet.remaining() shouldEqual PageSize
        rows <- resultSet.stream[IO].toList
        _ = ids(rows) shouldEqual Ids
      } yield ()
    }
  }
}

object PagingSpec {

  val PageSize = 4

  val Ids: List[Int] = (0 until 25).toList
}
