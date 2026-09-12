package com.evolutiongaming.scassandra

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import cats.~>
import com.datastax.driver.core.UserType
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MetadataSpec extends AnyWordSpec with Matchers {

  private val tableMetadata: TableMetadata = new TableMetadata {
    def name: String = "table"
  }

  private val keyspaceMetadata: KeyspaceMetadata[IO] = new KeyspaceMetadata[IO] {
    def name: String = "keyspace"
    def schema: IO[String] = IO.pure("schema")
    def asCql: IO[String] = IO.pure("cql")
    def table(name: String): IO[Option[TableMetadata]] = IO.pure(Option.when(name == "table")(tableMetadata))
    def tables: IO[List[TableMetadata]] = IO.pure(List(tableMetadata))
    def durableWrites: Boolean = true
    def virtual: Boolean = false
    def replication: IO[Map[String, String]] = IO.pure(Map("class" -> "SimpleStrategy"))
    def userTypes: IO[List[UserType]] = IO.pure(Nil)
  }

  private val metadata: Metadata[IO] = new Metadata[IO] {
    def clusterName: IO[String] = IO.pure("cluster")
    def keyspace(name: String): IO[Option[KeyspaceMetadata[IO]]] =
      IO.pure(Option.when(name == "keyspace")(keyspaceMetadata))
    def keyspaces: IO[List[KeyspaceMetadata[IO]]] = IO.pure(List(keyspaceMetadata))
    def schema: IO[String] = IO.pure("schema")
  }

  private def counting(counter: Ref[IO, Int]): IO ~> IO = new (IO ~> IO) {
    def apply[A](fa: IO[A]): IO[A] = counter.update(_ + 1) *> fa
  }

  "Metadata.mapK" should {

    "delegate every call through the transformation" in {
      val program = for {
        counter <- Ref[IO].of(0)
        metadata1 = metadata.mapK(counting(counter))
        clusterName <- metadata1.clusterName
        schema <- metadata1.schema
        keyspace <- metadata1.keyspace("keyspace")
        missing <- metadata1.keyspace("missing")
        keyspaces <- metadata1.keyspaces
        keyspaceSchema <- keyspace.get.schema
        count <- counter.get
      } yield (
        clusterName,
        schema,
        keyspace.map(_.name),
        missing,
        keyspaces.map(_.name),
        keyspaceSchema,
        count,
      )
      program.unsafeRunSync() shouldEqual
        (("cluster", "schema", Some("keyspace"), None, List("keyspace"), "schema", 6))
    }
  }

  "KeyspaceMetadata.mapK" should {

    "delegate every call through the transformation" in {
      val program = for {
        counter <- Ref[IO].of(0)
        keyspace1 = keyspaceMetadata.mapK(counting(counter))
        schema <- keyspace1.schema
        cql <- keyspace1.asCql
        table <- keyspace1.table("table")
        missing <- keyspace1.table("missing")
        tables <- keyspace1.tables
        replication <- keyspace1.replication
        userTypes <- keyspace1.userTypes
        count <- counter.get
      } yield (schema, cql, table.map(_.name), missing, tables.map(_.name), replication, userTypes, count)
      program.unsafeRunSync() shouldEqual
        (("schema", "cql", Some("table"), None, List("table"), Map("class" -> "SimpleStrategy"), Nil, 7))
      val keyspace1 = keyspaceMetadata.mapK(cats.arrow.FunctionK.id[IO])
      keyspace1.name shouldEqual "keyspace"
      keyspace1.durableWrites shouldEqual true
      keyspace1.virtual shouldEqual false
    }
  }
}
