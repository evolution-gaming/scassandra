package com.evolutiongaming.scassandra

import cats.effect.Sync
import cats.implicits._
import cats.{FlatMap, ~>}
import com.datastax.driver.core.{UserType, KeyspaceMetadata => KeyspaceMetadataJ, Metadata => MetadataJ, TableMetadata => TableMetadataJ}
import com.evolutiongaming.util.ToScala

trait Metadata[F[_]] {

  def clusterName: F[String]

  def keyspace(name: String): F[Option[KeyspaceMetadata[F]]]

  def keyspaces: F[List[KeyspaceMetadata[F]]]

  def schema: F[String]
}

object Metadata {

  def apply[F[_] : Sync](metadata: MetadataJ): Metadata[F] = {
    new Metadata[F] {

      val clusterName = Sync[F].delay { metadata.getClusterName }

      val schema = Sync[F].delay { metadata.exportSchemaAsString() }

      def keyspace(name: String) = {
        Sync[F].delay {
          for {
            keyspace <- Option(metadata.getKeyspace(name))
          } yield {
            KeyspaceMetadata(keyspace)
          }
        }
      }

      val keyspaces = {
        Sync[F].delay {
          for {
            keyspace <- ToScala.from(metadata.getKeyspaces).toList
          } yield {
            KeyspaceMetadata(keyspace)
          }
        }
      }
    }
  }


  implicit class MetadataOps[F[_]](val self: Metadata[F]) extends AnyVal {

    def mapK[G[_]](f: F ~> G)(implicit G: FlatMap[G]): Metadata[G] = new Metadata[G] {

      def clusterName = f(self.clusterName)

      def keyspace(name: String) = {
        for {
          a <- f(self.keyspace(name))
        } yield for {
          a <- a
        } yield {
          a.mapK(f)
        }
      }

      def keyspaces = {
        for {
          a <- f(self.keyspaces)
        } yield for {
          a <- a
        } yield {
          a.mapK(f)
        }
      }

      def schema = f(self.schema)
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

  def apply[F[_] : Sync](keyspaceMetadata: KeyspaceMetadataJ): KeyspaceMetadata[F] = {
    new KeyspaceMetadata[F] {

      val name = keyspaceMetadata.getName

      val schema = Sync[F].delay { keyspaceMetadata.exportAsString() }

      val asCql = Sync[F].delay { keyspaceMetadata.asCQLQuery() }

      def table(name: String) = {
        Sync[F].delay {
          for {
            tableMetadata <- Option(keyspaceMetadata.getTable(name))
          } yield {
            TableMetadata(tableMetadata)
          }
        }
      }

      val tables = {
        Sync[F].delay {
          for {
            tableMetadata <- ToScala.from(keyspaceMetadata.getTables).toList
          } yield {
            TableMetadata(tableMetadata)
          }
        }
      }

      val durableWrites = keyspaceMetadata.isDurableWrites

      val virtual = keyspaceMetadata.isVirtual

      val replication = {
        Sync[F].delay { ToScala.from(keyspaceMetadata.getReplication).toMap }
      }

      val userTypes = {
        Sync[F].delay { ToScala.from(keyspaceMetadata.getUserTypes).toList }
      }
    }
  }


  implicit class KeyspaceMetadataOps[F[_]](val self: KeyspaceMetadata[F]) extends AnyVal {

    def mapK[G[_]](f: F ~> G): KeyspaceMetadata[G] = new KeyspaceMetadata[G] {

      def name = self.name

      def schema = f(self.schema)

      def asCql = f(self.asCql)

      def table(name: String) = f(self.table(name))

      def tables = f(self.tables)

      def durableWrites = self.durableWrites

      def virtual = self.virtual

      def replication = f(self.replication)

      def userTypes = f(self.userTypes)
    }
  }
}


trait TableMetadata {
  def name: String
}

object TableMetadata {

  def apply(tableMetadata: TableMetadataJ): TableMetadata = new TableMetadata {
    val name = tableMetadata.getName
  }
}