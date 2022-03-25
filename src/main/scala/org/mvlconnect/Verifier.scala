package org.mvlconnect

import com.typesafe.scalalogging.Logger
import org.mvlconnect.model.CDCTable
import org.slf4j.LoggerFactory
import slick.dbio.DBIO

object Verifier extends DBUtils {

  def splitConf2Tables(tString : String) : Array[CDCTable] = {
    val tablesArr = tString
      .split(",")
      .map(_.trim.split("\\."))
    tablesArr.map(tbl => new CDCTable(tbl.head, tbl.last))
  }

  def verify(tables: Array[StreamTable]): Boolean = {
    val log = Logger(LoggerFactory.getLogger(this.getClass))
    val toExec = tables.map(tbl => tbl -> checkIfMVLogExists(tbl.table)).toMap
    val execSeq = DBIO.sequence(toExec.map(t => t._2))
    val resultSeq = exec(execSeq)
    var res = true
    for (elem <- resultSeq.zipWithIndex) {
      if (!elem._1.contains("1")) {
        log.error(s"Materialized view log for ${tables(elem._2).table.owner}.${tables(elem._2).table.tableName} not found")
        res = false
      }
    }
    res
  }
}
