package org.mvlconnect.connector

trait Constants {
  // version string constants
  val ver               = "0.1"

  val connectorClass    = "connector.class"

  // config string constants
  val configDBStr       = "orcl"
  val topicStr          = "topic"
  val processStr        = "processChanges"
  val flashbackQueryStr = "flashbackQuery"
  val timeoutStr        = "timeout"
  val urlStr            = "url"
  val userStr           = "user"

  // oracle string constants
  val mLogPrefixStr     = "MLOG$_"
  val yesStr            = "YES"
  val shortYesStr       = "Y"
  val shortNoStr        = "N"
  val dummyValStr       = "1"
  val logRowStr         = "M_ROW$$"
  val rowidStr          = "ROWID"
  val deleteOpStr       = "D"
  val insertOpStr       = "I"
  val updateOpStr       = "U"

  // oracle data types constants
  val varchar2Str       = "VARCHAR2"
  val numberStr         = "NUMBER"
  val clobStr           = "CLOB"
  val dateStr           = "DATE"
  val timestampStr      = "TIMESTAMP"
  val timestamp9Str     = "TIMESTAMP(9)"

  // schema string constants
  val tableStr          = "table"
  val SCNStr            = "SCN"
  val beforeSchemaStr   = "before"
  val afterSchemaStr    = "after"
  val operSchemaStr     = "op"
  val sourceStr         = "source"
  val connectorStr      = "connector"
  val versionStr        = "version"
  val dbStr             = "db"
  val schemaStr         = "schema"
}
