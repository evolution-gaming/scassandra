package com.evolutiongaming.scassandra

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import cats.syntax.all.*
import cats.~>
import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.`type`.UserDefinedType
import com.datastax.oss.driver.api.core.metadata.schema.{
  KeyspaceMetadata as KeyspaceMetadataJ,
  TableMetadata as TableMetadataJ,
}
import com.datastax.oss.driver.api.core.metadata.{Metadata as MetadataJ, Node}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.{Optional, UUID}
import scala.jdk.CollectionConverters.*

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
    def userTypes: IO[List[UserDefinedType]] = IO.pure(Nil)
  }

  private val metadata: Metadata[IO] = new Metadata[IO] {
    def clusterName: IO[Option[String]] = IO.pure(Some("cluster"))
    def keyspace(name: String): IO[Option[KeyspaceMetadata[IO]]] =
      IO.pure(Option.when(name == "keyspace")(keyspaceMetadata))
    def keyspaces: IO[List[KeyspaceMetadata[IO]]] = IO.pure(List(keyspaceMetadata))
    def nodes: IO[List[Node]] = IO.pure(Nil)
  }

  private def counting(counter: Ref[IO, Int]): IO ~> IO = new (IO ~> IO) {
    def apply[A](fa: IO[A]): IO[A] = counter.update(_ + 1) *> fa
  }

  private val tableJ: TableMetadataJ = ProxyMock[TableMetadataJ] {
    case ("getName", Nil) => CqlIdentifier.fromInternal("table")
  }

  private val keyspaceJ: KeyspaceMetadataJ = ProxyMock[KeyspaceMetadataJ] {
    case ("getName", Nil) => CqlIdentifier.fromInternal("keyspace")
    case ("isDurableWrites", Nil) => Boolean.box(true)
    case ("isVirtual", Nil) => Boolean.box(false)
    case ("getReplication", Nil) => Map("class" -> "SimpleStrategy").asJava
    case ("getTables", Nil) => Map(CqlIdentifier.fromInternal("table") -> tableJ).asJava
    case ("getTable", List(name: String)) =>
      Option.when(name == "table")(tableJ).fold(Optional.empty[TableMetadataJ])(Optional.of)
    case ("getUserDefinedTypes", Nil) => Map.empty[CqlIdentifier, UserDefinedType].asJava
    case ("describe", List(_)) => "cql"
    case ("describeWithChildren", List(_)) => "schema"
  }

  private val node: Node = ProxyMock[Node](PartialFunction.empty)

  private val metadataJ: MetadataJ = ProxyMock[MetadataJ] {
    case ("getClusterName", Nil) => Optional.of("cluster")
    case ("getKeyspaces", Nil) => Map(CqlIdentifier.fromInternal("keyspace") -> keyspaceJ).asJava
    case ("getKeyspace", List(name: String)) =>
      Option.when(name == "keyspace")(keyspaceJ).fold(Optional.empty[KeyspaceMetadataJ])(Optional.of)
    case ("getNodes", Nil) => Map(UUID.randomUUID() -> node).asJava
  }

  "Metadata" should {

    "wrap the driver metadata" in {
      val metadata = Metadata[IO](metadataJ)
      val program = for {
        clusterName <- metadata.clusterName
        keyspace <- metadata.keyspace("keyspace")
        missing <- metadata.keyspace("missing")
        keyspaces <- metadata.keyspaces
        nodes <- metadata.nodes
        schema <- keyspace.traverse(_.schema)
        cql <- keyspace.traverse(_.asCql)
        table <- keyspace.flatTraverse(_.table("table"))
        missingTable <- keyspace.flatTraverse(_.table("missing"))
        tables <- keyspace.traverse(_.tables)
        replication <- keyspace.traverse(_.replication)
        userTypes <- keyspace.traverse(_.userTypes)
      } yield {
        clusterName shouldEqual Some("cluster")
        keyspace.map(_.name) shouldEqual Some("keyspace")
        keyspace.map(_.durableWrites) shouldEqual Some(true)
        keyspace.map(_.virtual) shouldEqual Some(false)
        missing shouldEqual None
        keyspaces.map(_.name) shouldEqual List("keyspace")
        nodes shouldEqual List(node)
        schema shouldEqual Some("schema")
        cql shouldEqual Some("cql")
        table.map(_.name) shouldEqual Some("table")
        missingTable shouldEqual None
        tables.map(_.map(_.name)) shouldEqual Some(List("table"))
        replication shouldEqual Some(Map("class" -> "SimpleStrategy"))
        userTypes shouldEqual Some(Nil)
      }
      program.unsafeRunSync()
    }
  }

  "Metadata.mapK" should {

    "delegate every call through the transformation" in {
      val program = for {
        counter <- Ref[IO].of(0)
        metadata1 = metadata.mapK(counting(counter))
        clusterName <- metadata1.clusterName
        nodes <- metadata1.nodes
        keyspace <- metadata1.keyspace("keyspace")
        missing <- metadata1.keyspace("missing")
        keyspaces <- metadata1.keyspaces
        keyspaceSchema <- keyspace.traverse(_.schema)
        count <- counter.get
      } yield (
        clusterName,
        nodes,
        keyspace.map(_.name),
        missing,
        keyspaces.map(_.name),
        keyspaceSchema,
        count,
      )
      program.unsafeRunSync() shouldEqual
        ((Some("cluster"), Nil, Some("keyspace"), None, List("keyspace"), Some("schema"), 6))
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
