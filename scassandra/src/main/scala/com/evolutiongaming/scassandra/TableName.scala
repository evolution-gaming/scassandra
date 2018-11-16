package com.evolutiongaming.scassandra

final case class TableName(keyspace: String, table: String)

object TableName {

  implicit val ToCqlImpl: ToCql[TableName] = new ToCql[TableName] {
    def apply(a: TableName) = s"${ a.keyspace }.${ a.table }"
  }
}