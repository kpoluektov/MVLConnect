package org.mvlconnect.model

import org.apache.kafka.connect.data.{Schema, Timestamp}
import org.mvlconnect.connector.Constants

case class Column(name: String, cType: String, nullable: String ) extends Constants{
  def OraType = {
    cType match {
      case `varchar2Str` => classOf[String]
      case `numberStr` => classOf[Long]
    }
  }
  def isNullable = this.nullable == shortYesStr

  def getSchema : Schema =  this.cType match {
    case `varchar2Str` => if (this.isNullable) Schema.OPTIONAL_STRING_SCHEMA else Schema.STRING_SCHEMA
    case `clobStr` => Schema.OPTIONAL_STRING_SCHEMA
    case `numberStr` => if (this.isNullable) Schema.OPTIONAL_INT64_SCHEMA else Schema.INT64_SCHEMA
    case `dateStr` | `timestampStr` | `timestamp9Str` => if (this.isNullable) Timestamp.builder().optional().build() else Timestamp.SCHEMA
  }
}
