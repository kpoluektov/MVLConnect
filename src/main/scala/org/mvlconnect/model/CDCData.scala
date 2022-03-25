package org.mvlconnect.model

import org.mvlconnect.CDCOper
import org.mvlconnect.connector.Constants

import scala.collection.JavaConverters._


trait CDC extends Constants{
  def table : CDCTable
  def scn : MVL.SCNType
  def getOffSet = Map(SCNStr -> scn).asJava
}

case class CDCEmpty(table: CDCTable, scn : MVL.SCNType) extends CDC

case class CDCData (table: CDCTable, scn: MVL.SCNType, rows : Seq[ _ <: CDCOper])  extends CDC
