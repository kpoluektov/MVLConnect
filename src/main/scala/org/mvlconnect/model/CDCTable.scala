package org.mvlconnect.model

import org.mvlconnect.DBUtils
import scala.collection.JavaConverters._

case class CDCTable(owner: String, tableName: String) extends DBUtils {
  override def toString = s"$owner.$tableName"
  def getNameOnly:String = tableName
  var logTable: String = getMVLogName(this)
  def getLogName : String = logTable
  def getPartition : java.util.Map[String, String] = Map(tableStr -> this.toString).asJava
}
