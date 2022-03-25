package org.mvlconnect

import org.mvlconnect.model.{AllSumap, CDCTable, Change, ChangeID, MVL}
import slick.jdbc.OracleProfile.api._

abstract class LogTable[Model <: Change](tag : Tag, table: CDCTable) extends Table[Model](tag, Some(table.owner), table.getLogName){
  def dmlType = column[String]("DMLTYPE$$")
  def oldNew = column[String]("OLD_NEW$$")
  def changeVector = column[String]("CHANGE_VECTOR$$")
  def xid = column[MVL.XIDType]("XID$$")
}

class LogTableID (tag: Tag, table: CDCTable, IDColumn : String) extends LogTable[ChangeID](tag, table){
  def id = column[String](IDColumn)//("M_ROW$$")
  def * = (id, dmlType, oldNew, changeVector, xid) <> (ChangeID.tupled, ChangeID.unapply)
}

class AllSumapTable(tag: Tag) extends Table[AllSumap](tag, "ALL_SUMMAP"){
  def xid = column[MVL.XIDType]("XID")
  def commit_scn = column[MVL.SCNType]("COMMIT_SCN")
  def * = (xid, commit_scn) <> (AllSumap.tupled, AllSumap.unapply)
}
