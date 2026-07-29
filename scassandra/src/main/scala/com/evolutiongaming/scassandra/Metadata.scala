package com.evolutiongaming.scassandra

import cats.effect.Sync
import cats.implicits.*
import cats.{FlatMap, ~>}
import com.datastax.driver.core.{
  KeyspaceMetadata as KeyspaceMetadataJ,
  Metadata as MetadataJ,
  TableMetadata as TableMetadataJ,
  UserType,
}

import scala.jdk.CollectionConverters.*

trait Metadata[F[_]] {

  def clusterName: F[String]

  def keyspace(name: String): F[Option[KeyspaceMetadata[F]]]

  def keyspaces: F[List[KeyspaceMetadata[F]]]

  def schema: F[String]
}

object Metadata {

  def apply[F[_]: Sync](metadata: MetadataJ): Metadata[F] = {
    new Metadata[F] {

      override val clusterName: F[String] = Sync[F].delay { metadata.getClusterName }

      override val schema: F[String] = Sync[F].delay { metadata.exportSchemaAsString() }

      override def keyspace(name: String): F[Option[KeyspaceMetadata[F]]] = Sync[F].delay {
        Option(metadata.getKeyspace(name)).map(KeyspaceMetadata(_))
      }

      override val keyspaces: F[List[KeyspaceMetadata[F]]] = Sync[F].delay {
        metadata.getKeyspaces.asScala.view.map(KeyspaceMetadata(_)).toList
      }
    }
  }

  implicit class MetadataOps[F[_]](val self: Metadata[F]) extends AnyVal {

    def mapK[G[_]](
      f: F ~> G,
    )(implicit
      G: FlatMap[G],
    ): Metadata[G] = new Metadata[G] {

      override def clusterName: G[String] = f(self.clusterName)

      override def keyspace(name: String): G[Option[KeyspaceMetadata[G]]] = {
        for {
          a <- f(self.keyspace(name))
        } yield {
          for {
            a <- a
          } yield {
            a.mapK(f)
          }
        }
      }

      override def keyspaces: G[List[KeyspaceMetadata[G]]] = {
        for {
          a <- f(self.keyspaces)
        } yield {
          for {
            a <- a
          } yield {
            a.mapK(f)
          }
        }
      }

      override def schema: G[String] = f(self.schema)
    }
  }
}

trait KeyspaceMetadata[F[_]] {

  def name: String

  def schema: F[String]

  def asCql: F[String]

  def table(name: String): F[Option[TableMetadata]]

  def tables: F[List[TableMetadata]]

  def durableWrites: Boolean

  def virtual: Boolean

  def replication: F[Map[String, String]]

  def userTypes: F[List[UserType]]
}

object KeyspaceMetadata {

  def apply[F[_]: Sync](keyspaceMetadata: KeyspaceMetadataJ): KeyspaceMetadata[F] = {
    new KeyspaceMetadata[F] {

      override val name: String = keyspaceMetadata.getName

      override val schema: F[String] = Sync[F].delay { keyspaceMetadata.exportAsString() }

      override val asCql: F[String] = Sync[F].delay { keyspaceMetadata.asCQLQuery() }

      override def table(name: String): F[Option[TableMetadata]] = Sync[F].delay {
        Option(keyspaceMetadata.getTable(name)).map(TableMetadata(_))
      }

      override val tables: F[List[TableMetadata]] = Sync[F].delay {
        keyspaceMetadata.getTables.asScala.view.map(TableMetadata(_)).toList
      }

      override val durableWrites: Boolean = keyspaceMetadata.isDurableWrites

      override val virtual: Boolean = keyspaceMetadata.isVirtual

      override val replication: F[Map[String, String]] = Sync[F].delay {
        keyspaceMetadata.getReplication.asScala.toMap
      }

      override val userTypes: F[List[UserType]] = Sync[F].delay {
        keyspaceMetadata.getUserTypes.asScala.toList
      }
    }
  }

  implicit class KeyspaceMetadataOps[F[_]](val self: KeyspaceMetadata[F]) extends AnyVal {

    def mapK[G[_]](f: F ~> G): KeyspaceMetadata[G] = new KeyspaceMetadata[G] {

      override def name: String = self.name

      override def schema: G[String] = f(self.schema)

      override def asCql: G[String] = f(self.asCql)

      override def table(name: String): G[Option[TableMetadata]] = f(self.table(name))

      override def tables: G[List[TableMetadata]] = f(self.tables)

      override def durableWrites: Boolean = self.durableWrites

      override def virtual: Boolean = self.virtual

      override def replication: G[Map[String, String]] = f(self.replication)

      override def userTypes: G[List[UserType]] = f(self.userTypes)
    }
  }
}

trait TableMetadata {
  def name: String
}

object TableMetadata {

  def apply(tableMetadata: TableMetadataJ): TableMetadata = new TableMetadata {
    override val name: String = tableMetadata.getName
  }
}
