package org.mvlconnect.model

import slick.jdbc.GetResult

abstract class Change(
                     dmlType: String,
                     oldNew: String,
                     changeVector: String,
                     xid : MVL.XIDType
                     )

case class ChangeID(
                   id: String,
                   dmlType: String,
                   oldNew: String,
                   changeVector: String,
                   xid : MVL.XIDType
                 ) extends Change(dmlType, oldNew, changeVector, xid){
  override def equals(that: Any): Boolean = that match {
    case that: ChangeID => that.canEqual(this) && this.id == that.id && this.dmlType == that.dmlType
    case _ => false
  }
}

trait Changes{
  implicit def getResultChangeID : GetResult[ChangeID]  = GetResult(r => ChangeID(r.nextString, r.nextString, r.nextString,
    r.nextString, r.nextLong()))
}


final case class AllSumap(
                           xid : MVL.XIDType,
                           commit_scn : MVL.SCNType
                         )
trait AllSumaps{
  implicit def getResultAllSumap : GetResult[AllSumap]  = GetResult(r => AllSumap(r.nextLong, r.nextLong))
}

case class SCNRange(start:MVL.SCNType, stop :MVL.SCNType, processChanges: Boolean, flashbackQuery: Boolean)
