package com.evolutiongaming.scassandra

import com.datastax.driver.core.{ColumnDefinitions, ExecutionInfo, ResultSet, ResultSetFuture, Row}
import com.evolutiongaming.scassandra.MockSupport.notSupported
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture
import com.google.common.util.concurrent.{Futures, ListenableFuture}

import java.util.concurrent.TimeUnit
import java.util.{Iterator as IteratorJ, List as ListJ}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

final class ResultSetMock(pages: List[List[Row]]) extends ResultSet {

  private var remaining: List[List[Row]] = pages.drop(1)
  private val buffer: mutable.Queue[Row] = mutable.Queue.from(pages.headOption.getOrElse(Nil))
  private var fetches: Int = 0

  def fetchCount: Int = synchronized { fetches }

  override def isExhausted: Boolean = synchronized { buffer.isEmpty && remaining.isEmpty }

  override def isFullyFetched: Boolean = synchronized { remaining.isEmpty }

  override def getAvailableWithoutFetching: Int = synchronized { buffer.size }

  override def fetchMoreResults(): ListenableFuture[ResultSet] = synchronized {
    remaining match {
      case page :: rest =>
        fetches += 1
        remaining = rest
        buffer.enqueueAll(page)
      case Nil =>
    }
    Futures.immediateFuture(this)
  }

  override def one(): Row = synchronized { if (buffer.isEmpty) null else buffer.dequeue() }

  override def all(): ListJ[Row] = synchronized {
    while (!isFullyFetched) fetchMoreResults()
    buffer.dequeueAll(_ => true).asJava
  }

  override def iterator(): IteratorJ[Row] = new IteratorJ[Row] {
    override def hasNext: Boolean = !isExhausted
    override def next(): Row = ResultSetMock.this.synchronized {
      if (buffer.isEmpty) fetchMoreResults()
      buffer.dequeue()
    }
  }

  override def getExecutionInfo: ExecutionInfo = notSupported

  override def getAllExecutionInfo: ListJ[ExecutionInfo] = notSupported

  override def getColumnDefinitions: ColumnDefinitions = notSupported

  override def wasApplied(): Boolean = notSupported
}

object ResultSetMock {

  def apply(pages: List[Row]*): ResultSetMock = new ResultSetMock(pages.toList)

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

object ResultSetFutureMock {

  def apply(resultSet: ResultSet): ResultSetFuture = {
    new SimpleForwardingListenableFuture[ResultSet](Futures.immediateFuture(resultSet)) with ResultSetFuture {

      override def getUninterruptibly(): ResultSet = resultSet

      override def getUninterruptibly(timeout: Long, unit: TimeUnit): ResultSet = resultSet
    }
  }
}
