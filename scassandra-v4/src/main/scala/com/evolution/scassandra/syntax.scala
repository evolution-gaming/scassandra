package com.evolution.scassandra

import cats.effect.Async
import cats.effect.syntax.all.*
import cats.syntax.all.*
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, Row, SimpleStatement, Statement}
import com.datastax.oss.driver.api.core.data.{
  GettableByIndex,
  GettableByName,
  SettableByIndex,
  SettableByName,
}
import com.evolution.scassandra.util.FromCompletionStage
import com.evolutiongaming.sstream.FoldWhile.*
import com.evolutiongaming.sstream.Stream

import scala.jdk.CollectionConverters.*

object syntax {

  implicit class ScassandraAsyncResultSetOps(val self: AsyncResultSet) extends AnyVal {

    /**
     * Streams the rows of all the pages, starting from the current one.
     *
     * The next page is fetched in the background, while the rows of the current page are
     * processed.
     *
     * Note, that the pages of [[AsyncResultSet]] can be iterated only once, so the stream
     * should be consumed once as well.
     */
    def stream[F[_]: Async]: Stream[F, Row] = AsyncResultSetStream(self)
  }

  implicit class ScassandraSessionStreamOps[F[_]](val self: CassandraSession[F]) extends AnyVal {

    def executeStream(
      statement: Statement[?],
    )(implicit
      F: Async[F],
    ): Stream[F, Row] = {
      for {
        resultSet <- Stream.lift(self.execute(statement))
        row <- resultSet.stream[F]
      } yield row
    }

    def executeStream(
      query: String,
    )(implicit
      F: Async[F],
    ): Stream[F, Row] = {
      executeStream(SimpleStatement.newInstance(query))
    }
  }

  implicit class ScassandraSettableByNameOps[A <: SettableByName[A]](val self: A) extends AnyVal {

    def encode[B](
      name: String,
      value: B,
    )(implicit
      encode: EncodeByName[B],
    ): A = {
      encode(self, name, value)
    }

    def encode[B](
      value: B,
    )(implicit
      encode: EncodeRow[B],
    ): A = {
      encode(self, value)
    }

    def encodeSome[B](
      name: String,
      value: Option[B],
    )(implicit
      encode: EncodeByName[B],
    ): A = {
      value.fold(self)(encode(self, name, _))
    }

    def encodeSome[B](
      value: Option[B],
    )(implicit
      encode: EncodeRow[B],
    ): A = {
      value.fold(self)(encode(self, _))
    }
  }

  implicit class ScassandraSettableByIndexOps[A <: SettableByIndex[A]](val self: A) extends AnyVal {

    def encodeAt[B](
      idx: Int,
      value: B,
    )(implicit
      encode: EncodeByIdx[B],
    ): A = {
      encode(self, idx, value)
    }
  }

  implicit class ScassandraGettableByNameOps(val self: GettableByName) extends AnyVal {

    def decode[A](
      name: String,
    )(implicit
      decode: DecodeByName[A],
    ): A = {
      decode(self, name)
    }

    def decode[A](
      implicit
      decode: DecodeRow[A],
    ): A = {
      decode(self)
    }
  }

  implicit class ScassandraGettableByIndexOps(val self: GettableByIndex) extends AnyVal {

    def decodeAt[A](
      idx: Int,
    )(implicit
      decode: DecodeByIdx[A],
    ): A = {
      decode(self, idx)
    }
  }

  implicit class ScassandraToCqlOps[A](val self: A) extends AnyVal {

    def toCql(
      implicit
      toCql: ToCql[A],
    ): String = toCql(self)
  }

  implicit class ScassandraUpdateByNameSyntax[D <: GettableByName & SettableByName[D]](val data: D)
  extends AnyVal {

    def update[A](
      value: A,
    )(implicit
      update: UpdateRow[A],
    ): D = {
      update(data, value)
    }

    def update[A](
      name: String,
      value: A,
    )(implicit
      update: UpdateByName[A],
    ): D = {
      update(data, name, value)
    }
  }

  implicit class ScassandraUpdateByIndexSyntax[D <: GettableByIndex & SettableByIndex[D]](val data: D)
  extends AnyVal {

    def updateAt[A](
      idx: Int,
      value: A,
    )(implicit
      update: UpdateByIdx[A],
    ): D = {
      update(data, idx, value)
    }
  }

  private object AsyncResultSetStream {

    type Step[F[_], L, R] = (L, Row) => F[Either[L, R]]

    type Iteration[L, R] = Either[(AsyncResultSet, L), Either[L, R]]

    def apply[F[_]: Async](resultSet: AsyncResultSet): Stream[F, Row] = new Stream[F, Row] {

      override def foldWhileM[L, R](l: L)(f: (L, Row) => F[Either[L, R]]): F[Either[L, R]] = {
        (resultSet, l).tailRecM[F, Either[L, R]] {
          case (resultSet, state) => foldPage(resultSet, state, f)
        }
      }
    }

    private def foldPage[F[_]: Async, L, R](
      resultSet: AsyncResultSet,
      state: L,
      step: Step[F, L, R],
    ): F[Iteration[L, R]] = {
      for {
        rows <- Async[F].delay { resultSet.currentPage().asScala.toList }
        result <-
          if (resultSet.hasMorePages) foldAndFetchNext(resultSet, rows, state, step)
          else rows.foldWhileM(state)(step).map { _.asRight[(AsyncResultSet, L)] }
      } yield result
    }

    private def foldAndFetchNext[F[_]: Async, L, R](
      resultSet: AsyncResultSet,
      rows: List[Row],
      state: L,
      step: Step[F, L, R],
    ): F[Iteration[L, R]] = {
      for {
        fetching <- FromCompletionStage { resultSet.fetchNextPage() }.start
        result <- rows.foldWhileM(state)(step)
        iteration <- result match {
          case Left(state) => fetching.joinWithNever.map { next => (next, state).asLeft[Either[L, R]] }
          case Right(result) => fetching.cancel.as(result.asRight[L].asRight[(AsyncResultSet, L)])
        }
      } yield iteration
    }
  }
}
