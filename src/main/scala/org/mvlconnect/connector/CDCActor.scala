package org.mvlconnect.connector

import akka.actor.Actor
import org.mvlconnect.model.{CDCEmpty, ChangeID, SCNRange}
import org.mvlconnect.{DBUtils, StreamTable}
import slick.jdbc.OracleProfile.api._


class CDCActor(sTable : StreamTable) extends Actor with DBUtils{
  def receive = {
    case x: SCNRange => {
      exec(sTable.getChanges(x).result) match{
        case ch : Seq[ChangeID] if (ch.size > 0) =>
          val resChanges = if (x.processChanges) sTable.processChanges(ch) else ch.distinct
          sender() ! sTable.getCDCData(resChanges, x.stop, x.flashbackQuery)
        case _ => sender() ! CDCEmpty(sTable.table, x.stop)
      }
    }
    case _ => throw new Exception("Wrong XID for table " + sTable.table.tableName)
  }
}
