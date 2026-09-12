package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, ColumnDefinitions, ExecutionInfo, Row}
import com.evolutiongaming.scassandra.MockSupport.notSupported

import java.lang.Iterable as IterableJ
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.{ArrayDeque, Iterator as IteratorJ}
import scala.jdk.CollectionConverters.*

final class ResultSetMock private (pages: List[List[Row]], fetches: AtomicInteger) extends AsyncResultSet {

  private val rows = new ArrayDeque[Row](pages.headOption.getOrElse(Nil).asJava)

  def fetchCount: Int = fetches.get()

  override def remaining(): Int = rows.size()

  override def currentPage(): IterableJ[Row] = () =>
    new IteratorJ[Row] {
      override def hasNext: Boolean = !rows.isEmpty
      override def next(): Row = rows.poll()
    }

  override def one(): Row = rows.poll()

  override def hasMorePages: Boolean = pages.drop(1).nonEmpty

  override def fetchNextPage(): CompletionStage[AsyncResultSet] = {
    fetches.incrementAndGet()
    CompletableFuture.completedFuture(new ResultSetMock(pages.drop(1), fetches))
  }

  override def getColumnDefinitions: ColumnDefinitions = notSupported

  override def getExecutionInfo: ExecutionInfo = notSupported

  override def wasApplied(): Boolean = notSupported
}

object ResultSetMock {

  def apply(pages: List[Row]*): ResultSetMock = new ResultSetMock(pages.toList, new AtomicInteger(0))

  def rows(ids: Int*): List[Row] = ids.toList.map(RowMock(_))
}

object RowMock {

  def apply(id: Int): Row = ProxyMock[Row] {
    case ("getInt", _) => Int.box(id)
    case ("toString", Nil) => s"RowMock($id)"
    case ("hashCode", Nil) => Int.box(id)
  }

  def id(row: Row): Int = row.getInt(0)
}
