package com.evolutiongaming.scassandra

import cats.Functor
import cats.effect.Sync
import cats.implicits.*
import cats.~>
import com.datastax.oss.driver.api.core.`type`.UserDefinedType
import com.datastax.oss.driver.api.core.metadata.schema.{
  KeyspaceMetadata as KeyspaceMetadataJ,
  TableMetadata as TableMetadataJ,
}
import com.datastax.oss.driver.api.core.metadata.{Metadata as MetadataJ, Node}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

/**
 * See [[com.datastax.oss.driver.api.core.metadata.Metadata]]
 */
trait Metadata[F[_]] {

  def clusterName: F[Option[String]]

  def keyspace(name: String): F[Option[KeyspaceMetadata[F]]]

  def keyspaces: F[List[KeyspaceMetadata[F]]]

  def nodes: F[List[Node]]
}

object Metadata {

  def apply[F[_]: Sync](metadata: MetadataJ): Metadata[F] = {
    new Metadata[F] {

      override val clusterName: F[Option[String]] = Sync[F].delay { metadata.getClusterName.toScala }

      override def keyspace(name: String): F[Option[KeyspaceMetadata[F]]] = Sync[F].delay {
        metadata.getKeyspace(name).toScala.map(KeyspaceMetadata[F](_))
      }

      override val keyspaces: F[List[KeyspaceMetadata[F]]] = Sync[F].delay {
        metadata.getKeyspaces.values().asScala.view.map(KeyspaceMetadata[F](_)).toList
      }

      override val nodes: F[List[Node]] = Sync[F].delay { metadata.getNodes.values().asScala.toList }
    }
  }

  implicit class MetadataOps[F[_]](val self: Metadata[F]) extends AnyVal {

    def mapK[G[_]: Functor](f: F ~> G): Metadata[G] = new Metadata[G] {

      override def clusterName: G[Option[String]] = f(self.clusterName)

      override def keyspace(name: String): G[Option[KeyspaceMetadata[G]]] = {
        f(self.keyspace(name)).map(_.map(_.mapK(f)))
      }

      override def keyspaces: G[List[KeyspaceMetadata[G]]] = f(self.keyspaces).map(_.map(_.mapK(f)))

      override def nodes: G[List[Node]] = f(self.nodes)
    }
  }
}

/**
 * See [[com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata]]
 */
trait KeyspaceMetadata[F[_]] {

  def name: String

  def schema: F[String]

  def asCql: F[String]

  def table(name: String): F[Option[TableMetadata]]

  def tables: F[List[TableMetadata]]

  def durableWrites: Boolean

  def virtual: Boolean

  def replication: F[Map[String, String]]

  def userTypes: F[List[UserDefinedType]]
}

object KeyspaceMetadata {

  def apply[F[_]: Sync](keyspaceMetadata: KeyspaceMetadataJ): KeyspaceMetadata[F] = {
    new KeyspaceMetadata[F] {

      override val name: String = keyspaceMetadata.getName.asInternal

      override val schema: F[String] = Sync[F].delay { keyspaceMetadata.describeWithChildren(true) }

      override val asCql: F[String] = Sync[F].delay { keyspaceMetadata.describe(true) }

      override def table(name: String): F[Option[TableMetadata]] = Sync[F].delay {
        keyspaceMetadata.getTable(name).toScala.map(TableMetadata(_))
      }

      override val tables: F[List[TableMetadata]] = Sync[F].delay {
        keyspaceMetadata.getTables.values().asScala.view.map(TableMetadata(_)).toList
      }

      override val durableWrites: Boolean = keyspaceMetadata.isDurableWrites

      override val virtual: Boolean = keyspaceMetadata.isVirtual

      override val replication: F[Map[String, String]] = Sync[F].delay {
        keyspaceMetadata.getReplication.asScala.toMap
      }

      override val userTypes: F[List[UserDefinedType]] = Sync[F].delay {
        keyspaceMetadata.getUserDefinedTypes.values().asScala.toList
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

      override def userTypes: G[List[UserDefinedType]] = f(self.userTypes)
    }
  }
}

trait TableMetadata {
  def name: String
}

object TableMetadata {

  def apply(tableMetadata: TableMetadataJ): TableMetadata = new TableMetadata {
    override val name: String = tableMetadata.getName.asInternal
  }
}
