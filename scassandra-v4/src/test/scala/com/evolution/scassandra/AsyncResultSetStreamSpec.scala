package com.evolution.scassandra

import cats.effect.unsafe.implicits.global
import cats.effect.{Deferred, IO}
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, ColumnDefinitions, ExecutionInfo, Row}
import com.datastax.oss.driver.internal.core.cql.{DefaultColumnDefinitions, DefaultRow}
import com.evolution.scassandra.syntax.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.{CompletableFuture, CompletionStage, CountDownLatch, TimeUnit}
import scala.jdk.CollectionConverters.*

class AsyncResultSetStreamSpec extends AnyFunSuite with Matchers {

  import AsyncResultSetStreamSpec.*

  test("stream reads all the pages") {
    val lastPage = AsyncResultSetStub(rows(2), next = None)
    val firstPage = AsyncResultSetStub(rows(3), next = Some(CompletableFuture.completedFuture(lastPage)))

    firstPage.stream[IO].toList.unsafeRunSync().size shouldEqual 5
  }

  test("failure to fetch the next page fails the stream") {
    val error = new RuntimeException("fetch failed")
    val firstPage = AsyncResultSetStub(rows(1), next = Some(CompletableFuture.failedFuture(error)))

    firstPage.stream[IO].toList.attempt.unsafeRunSync() shouldEqual Left(error)
  }

  test("fetching of the next page is cancelled on early stop") {
    val nextPage = new CompletableFuture[AsyncResultSet]
    val firstPage = AsyncResultSetStub(rows(2), next = Some(nextPage))

    val result = firstPage.stream[IO].foldWhileM(()) { (_, _) =>
      firstPage.awaitFetch.as(Right("stop"))
    }.unsafeRunSync()

    result shouldEqual Right("stop")
    nextPage.isCancelled shouldEqual true
  }

  test("fetching of the next page is cancelled on failure") {
    val error = new RuntimeException("step failed")
    val nextPage = new CompletableFuture[AsyncResultSet]
    val firstPage = AsyncResultSetStub(rows(2), next = Some(nextPage))

    val result = firstPage.stream[IO].foldWhileM(()) { (_, _) =>
      firstPage.awaitFetch >> IO.raiseError[Either[Unit, Unit]](error)
    }

    result.attempt.unsafeRunSync() shouldEqual Left(error)
    nextPage.isCancelled shouldEqual true
  }

  test("fetching of the next page is cancelled on cancellation") {
    val nextPage = new CompletableFuture[AsyncResultSet]
    val firstPage = AsyncResultSetStub(rows(2), next = Some(nextPage))

    val program = for {
      folding <- Deferred[IO, Unit]
      fiber <- firstPage.stream[IO].foldWhileM(()) { (_, _) =>
        firstPage.awaitFetch >> folding.complete(()) >> IO.never
      }.start
      _ <- folding.get
      _ <- fiber.cancel
    } yield ()
    program.unsafeRunSync()

    nextPage.isCancelled shouldEqual true
  }
}

object AsyncResultSetStreamSpec {

  private val columns: ColumnDefinitions = DefaultColumnDefinitions.valueOf(List.empty.asJava)

  def rows(count: Int): List[Row] = List.fill(count)(new DefaultRow(columns, List.empty.asJava))

  /**
   * Page of the results, which iterates its rows once, as the driver does.
   *
   * The background fetch of the next page might be cancelled before it is requested, so
   * the tests wait for the request with [[awaitFetch]] to check its cancellation.
   */
  final case class AsyncResultSetStub(rows: List[Row], next: Option[CompletableFuture[AsyncResultSet]])
  extends AsyncResultSet {

    private val iterator = rows.iterator.asJava

    private val fetchRequested = new CountDownLatch(1)

    def awaitFetch: IO[Unit] = IO.blocking { fetchRequested.await(5, TimeUnit.SECONDS) }.void

    override def getColumnDefinitions: ColumnDefinitions = columns

    override def getExecutionInfo: ExecutionInfo = throw new UnsupportedOperationException

    override def remaining(): Int = throw new UnsupportedOperationException

    override def currentPage(): java.lang.Iterable[Row] = () => iterator

    override def hasMorePages: Boolean = next.isDefined

    override def fetchNextPage(): CompletionStage[AsyncResultSet] = {
      fetchRequested.countDown()
      next.getOrElse(throw new IllegalStateException("No next page"))
    }

    override def wasApplied(): Boolean = true
  }
}
