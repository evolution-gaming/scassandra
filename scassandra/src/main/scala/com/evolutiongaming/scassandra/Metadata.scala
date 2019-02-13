package com.evolutiongaming.scassandra

import com.datastax.driver.core.{KeyspaceMetadata => KeyspaceMetadataJ, Metadata => MetadataJ, TableMetadata => TableMetadataJ}

import scala.collection.JavaConverters._

trait Metadata {

  def clusterName: String

  def keyspace(name: String): Option[KeyspaceMetadata]

  def keyspaces(): List[KeyspaceMetadata]

  def schema(): String
}

object Metadata {

  def apply(metadata: MetadataJ): Metadata = new Metadata {

    def clusterName = metadata.getClusterName

    def schema() = metadata.exportSchemaAsString()

    def keyspace(name: String) = {
      for {
        keyspace <- Option(metadata.getKeyspace(name))
      } yield {
        KeyspaceMetadata(keyspace)
      }
    }

    def keyspaces() = {
      for {
        keyspace <- metadata.getKeyspaces.asScala.toList
      } yield {
        KeyspaceMetadata(keyspace)
      }
    }
  }
}


trait KeyspaceMetadata {

  def name: String

  def schema(): String

  def asCql(): String

  def table(name: String): Option[TableMetadata]

  def tables(): List[TableMetadata]

  def durableWrites: Boolean

  def virtual: Boolean

  def replication: Map[String, String]
}

object KeyspaceMetadata {

  def apply(keyspaceMetadata: KeyspaceMetadataJ): KeyspaceMetadata = new KeyspaceMetadata {

    def name = keyspaceMetadata.getName

    def schema() = keyspaceMetadata.exportAsString()

    def asCql() = keyspaceMetadata.asCQLQuery()

    def table(name: String) = {
      for {
        tableMetadata <- Option(keyspaceMetadata.getTable(name))
      } yield {
        TableMetadata(tableMetadata)
      }
    }

    def tables() = for {
      tableMetadata <- keyspaceMetadata.getTables.asScala.toList
    } yield {
      TableMetadata(tableMetadata)
    }

    def durableWrites = keyspaceMetadata.isDurableWrites

    def virtual = keyspaceMetadata.isVirtual

    def replication = keyspaceMetadata.getReplication.asScala.toMap
  }
}


trait TableMetadata {
  def name: String
}

object TableMetadata {

  def apply(tableMetadata: TableMetadataJ): TableMetadata = new TableMetadata {
    def name = tableMetadata.getName
  }
}