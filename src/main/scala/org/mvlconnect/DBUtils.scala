package org.mvlconnect

import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import org.mvlconnect.connector.Constants
import org.mvlconnect.model.CDCTable
import org.slf4j.LoggerFactory
import slick.dbio.DBIO
import slick.jdbc.OracleProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._

trait DBUtils extends OracleSQL with Constants{
  private val log = Logger(LoggerFactory.getLogger(this.getClass))

  val AllSumaps = TableQuery[AllSumapTable]

  def exec[T](action: DBIO[T]): T = Await.result(DBUtils.db.get.run(action), DBUtils.dbTimeout.seconds)

  def getMVLogName(table: CDCTable) = mLogPrefixStr + table.tableName

  def checkIfMVLogExists(table: CDCTable) = {
    checkMVLExists(table)
  }

  def init(conf: Config) = {
    DBUtils.db = Some(Database.forConfig(configDBStr, conf))
    DBUtils.dbTimeout = conf.getInt(timeoutStr)
    exec(checkConnection) match {
      case k : Vector[String] => k.head == dummyValStr
      case _ => false
    }
  }
  def DBClose = {
    log.info("Closing Database")
    DBUtils.db.get.close()
  }

  def getCurrentSCNValue = exec(getCurrentSCN).head

  def getPartition(table: CDCTable) = table.owner + "." + table.tableName
}

object DBUtils{
  // everything to be rewritten on connector init
  var db : Option[Database] = Some(null)
  var dbTimeout = 100
}