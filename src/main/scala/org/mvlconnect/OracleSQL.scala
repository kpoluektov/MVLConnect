package org.mvlconnect

import org.mvlconnect.model.{CDCTable, MVL}
import slick.jdbc.OracleProfile.api._

trait OracleSQL {
  val checkConnection = sql"select 1 from dual".as[String]
  def checkMVLExists(t: CDCTable) = sql"select 1 from all_tables where owner = ${t.owner} and table_name = ${t.getLogName}".as[String]
  def getColumnList(t: CDCTable) = sql"select column_name, data_type, nullable from all_tab_columns where owner = ${t.owner} and table_name = ${t.tableName} order by column_id".as[(String, String, String)]
  def getIsPKLog(t: CDCTable) = sql"select rowids, primary_key from all_mview_logs where log_owner = ${t.owner} and  master = ${t.tableName}".as[(String, String)]
  def getPKConstraintName(t: CDCTable) = sql"select constraint_name from all_constraints where constraint_type = 'P' and owner = ${t.owner} and table_name = ${t.tableName}".as[String]
  def getPKColumnName(t: StreamTable) = sql"select column_name from all_ind_columns where table_owner = ${t.table.owner} and table_name = ${t.table.tableName} and index_name = ${t.consNamePK}".as[String]
  def getCurrentSCN = sql"select current_scn from v$$database".as[MVL.SCNType]
}
