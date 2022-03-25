package org.mvlconnect.schema

import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaBuilder
import org.mvlconnect.connector.Constants

trait MVLSchema extends Constants {
  val  schemaData : Schema = Schema.OPTIONAL_STRING_SCHEMA
  val  schemaBuilder : SchemaBuilder = SchemaBuilder.struct.name(getClass().getName)
  val s = schemaBuilder.field("test", schemaData)

  def getSourceSchema : Schema = {
    val sourceSchemaBilder : SchemaBuilder = SchemaBuilder.struct.name("MVLSourceSchema")
    sourceSchemaBilder.field(SCNStr, Schema.INT64_SCHEMA)
    sourceSchemaBilder.field(connectorStr, Schema.STRING_SCHEMA)
    sourceSchemaBilder.field(versionStr, Schema.STRING_SCHEMA)
    sourceSchemaBilder.field(dbStr, Schema.STRING_SCHEMA)
    sourceSchemaBilder.field(schemaStr, Schema.STRING_SCHEMA)
    sourceSchemaBilder.field(tableStr, Schema.STRING_SCHEMA)
    sourceSchemaBilder.optional().build()
  }
}
