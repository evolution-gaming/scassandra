package com.evolutiongaming.scassandra

final case class TableName(keyspace: String, table: String)

object TableName {

  implicit val toCqlTableName: ToCql[TableName] = (a: TableName) => s"${ a.keyspace }.${ a.table }"
}