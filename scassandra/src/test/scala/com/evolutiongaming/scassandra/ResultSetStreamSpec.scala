package com.evolutiongaming.scassandra

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.datastax.driver.core.{ResultSet, Row, SimpleStatement, Statement}
import com.evolutiongaming.scassandra.StreamingCassandraSession.*
import com.evolutiongaming.scassandra.syntax.*
import com.evolutiongaming.sstream.Stream
import com.evolutiongaming.sstream.Stream.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


class ResultSetStreamSpec extends AnyWordSpec with Matchers {

  private def session(resultSet: ResultSet, executed: Statement => Unit = _ => ()): CassandraSession[IO] = {
    new CassandraSessionMock {
      override def execute(statement: Statement): IO[ResultSet] = IO {
        executed(statement)
        resultSet
      }
    }
  }

  private def ids(rows: List[Row]): List[Int] = rows.map(RowMock.id)

  private val streams: List[(String, ResultSetMock => Stream[IO, Row])] = List(
    ("ResultSet.stream", resultSet => resultSet.stream[IO]),
    ("executeStream(statement)", resultSet => session(resultSet).executeStream(new SimpleStatement("SELECT 1"))),
    ("executeStream(query)", resultSet => session(resultSet).executeStream("SELECT 1")),
  )

  for {
    (name, stream) <- streams
  } {
    name should {

      "return no rows for an empty result set" in {
        val resultSet = ResultSetMock()
        stream(resultSet).toList.unsafeRunSync() shouldEqual Nil
        resultSet.fetchCount shouldEqual 0
      }

      "return all rows of a single page without fetching" in {
        val resultSet = ResultSetMock(ResultSetMock.rows(1, 2, 3))
        ids(stream(resultSet).toList.unsafeRunSync()) shouldEqual List(1, 2, 3)
        resultSet.fetchCount shouldEqual 0
      }

      "return all rows across pages in order, fetching every next page once" in {
        val resultSet = ResultSetMock(ResultSetMock.rows(1, 2), ResultSetMock.rows(3), ResultSetMock.rows(4, 5))
        ids(stream(resultSet).toList.unsafeRunSync()) shouldEqual List(1, 2, 3, 4, 5)
        resultSet.fetchCount shouldEqual 2
      }

      "skip empty pages" in {
        val resultSet = ResultSetMock(Nil, ResultSetMock.rows(1), Nil, ResultSetMock.rows(2))
        ids(stream(resultSet).toList.unsafeRunSync()) shouldEqual List(1, 2)
        resultSet.fetchCount shouldEqual 3
      }

      "stop when the fold completes" in {
        val resultSet = ResultSetMock(ResultSetMock.rows(1, 2), ResultSetMock.rows(3))
        val result = stream(resultSet).foldWhileM(List.empty[Int]) { (seen, row) =>
          val seen1 = RowMock.id(row) :: seen
          IO.pure(if (seen1.size == 2) Right(seen1.reverse) else Left(seen1))
        }
        result.unsafeRunSync() shouldEqual Right(List(1, 2))
      }

      "return the first row" in {
        val resultSet = ResultSetMock(ResultSetMock.rows(1, 2), ResultSetMock.rows(3))
        stream(resultSet).first.unsafeRunSync().map(RowMock.id) shouldEqual Some(1)
      }
    }
  }

  "executeStream(query)" should {

    "wrap the query into a SimpleStatement" in {
      var executed = Option.empty[Statement]
      session(ResultSetMock(), statement => executed = Some(statement)).executeStream("SELECT 1").toList.unsafeRunSync()
      executed.map(_.asInstanceOf[SimpleStatement].getQueryString) shouldEqual Some("SELECT 1")
    }
  }

  "executeStream(statement)" should {

    "execute the statement as is" in {
      var executed = Option.empty[Statement]
      val statement = new SimpleStatement("SELECT 1")
      session(ResultSetMock(), statement => executed = Some(statement)).executeStream(statement).toList.unsafeRunSync()
      executed.map(_ eq statement) shouldEqual Some(true)
    }
  }

}
