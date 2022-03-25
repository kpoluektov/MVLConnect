package org.mvlconnect

import java.sql.{Date, ResultSet}

import akka.actor.ActorRef
import org.apache.kafka.connect.data.{Schema, SchemaBuilder}
import org.mvlconnect.model.{CDCData, CDCTable, ChangeID, Column, MVL, SCNRange}
import slick.lifted.{TableQuery, Tag}
import slick.jdbc.OracleProfile.api._
import slick.jdbc.{GetResult, PositionedParameters, PositionedResult, SetParameter}
import slick.sql.SqlStreamingAction

import scala.util.Try

class StreamTable(val table: CDCTable) extends DBUtils {

  var columns : Map[String, Column] = Map()
  var hasPK : Boolean = true
  val logTable: TableQuery[LogTableID] = TableQuery[LogTableID]((tag:Tag) => new LogTableID(tag, table, columnsPKLog.head))
  var consNamePK = ""
  var columnsPK : Vector[String] = Vector()
  var columnsPKLog : Vector[String] = Vector()
  var actor : Option[ActorRef] = Some(null)
  var initSCN : MVL.SCNType = 0

  def getChanges(range : SCNRange): Query[LogTableID, ChangeID, Seq] = {
    for {
      (log, s) <- logTable join AllSumaps.filter(_.commit_scn > range.start).filter(_.commit_scn <= range.stop) on (_.xid === _.xid)
    } yield log
  }


  def getObject(rs: ResultSet, k: Integer) : Any = {
    rs.getObject(k) match {
      case v : java.math.BigDecimal => Try(v.longValue()).get
      case t : oracle.sql.TIMESTAMP => new Date(t.dateValue().getTime)
      case r : oracle.sql.ROWID => r.stringValue()
      case other => other
    }
  }

  object ResultMap extends GetResult[Map[String,Any]] {
    def apply(pr: PositionedResult) = {
      val rs = pr.rs // <- jdbc result set
      val md = rs.getMetaData
      val res = (1 to pr.numColumns).map{ i=> md.getColumnName(i) ->
          getObject(rs, i) }.toMap
      pr.nextRow
      res
    }
  }

  implicit val setChangeSeqParameter: SetParameter[Seq[ChangeID]] = (v1: Seq[ChangeID], v2: PositionedParameters) => {
    if (hasPK) // TODO add parse column type
      v1.map(_.id.toDouble).foreach(v2.setDouble)
    else
      v1.map(_.id).foreach(v2.setString)
  }

  def processChanges(changes : Seq[ChangeID]) : Seq[ChangeID] = {
    // suppose
    // if deletes exists, all others changes ignored
    // all others changes should be grouped
    //val res : List[ChangeID] = changes.distinct
    changes.groupBy(ch => ch.id).map(t => {
      if(t._2.exists(_.dmlType == deleteOpStr)) ChangeID(t._1, deleteOpStr, null, null, t._2.head.xid)
      else ChangeID(t._1, updateOpStr, null, null, t._2.head.xid)
    }
    ).toList
  }

  def getRecords(changes : Seq[ChangeID], scn: MVL.SCNType, flashbackQuery: Boolean): SqlStreamingAction[Vector[Map[String, Any]], Map[String, Any], Effect] = {
    val sql = if(flashbackQuery)
        sql"select t.* #${if(!hasPK)", rowid" else ""} from #${table.owner}.#${table.tableName} as of scn $scn t where #${columnsPK.head} in ($changes#${",?" * (changes.size - 1)})"
      else
        sql"select t.* #${if(!hasPK)", rowid" else ""} from #${table.owner}.#${table.tableName} t where #${columnsPK.head} in ($changes#${",?" * (changes.size - 1)})"
    sql.as(ResultMap)
  }

  def pkCast(id: Any) : Any = {
    (id, columns(columnsPK.head).cType) match {
      case (x : Double, "NUMBER")  => x
      case (x : String, "NUMBER")  => x.toLong
      case (x : Double, "VARCHAR2")  => x.toString
      case x  => x
    }
  }

  def getCDCData(changes : Seq[ChangeID], scn: MVL.SCNType, flashbackQuery: Boolean) : CDCData = {
    val chGroups = changes.groupBy(_.dmlType)
    var opers : Seq[ _ <: CDCOper] = Seq()
    chGroups.map( op => op._1 match {
      case `deleteOpStr` => op._2.map(c =>
        opers = opers ++ Seq(CDCDelete(columnsPK.head, pkCast(c.id), schemaShort)(table, scn)))
      case `insertOpStr` =>
        val data = exec(getRecords(op._2, scn, flashbackQuery))
        data.map(c =>
          opers = opers ++ Seq(CDCInsert(schemaFull, c)(table, scn)))
      case `updateOpStr` =>
        val data = exec(getRecords(op._2, scn, flashbackQuery))
        data.map(c =>
          opers = opers ++ Seq(CDCUpdate(columnsPK.head, schemaShort, schemaFull, c)(table, scn)))
      case _ => throw new Exception ("Unknown type of DML operation on table "+ table)
  })
    CDCData(table, scn, opers)
  }

  var schemaShort: Schema = Schema.OPTIONAL_STRING_SCHEMA
  var schemaFull: Schema = Schema.OPTIONAL_STRING_SCHEMA

  def isNullable(c: Column): Boolean = c.nullable == shortYesStr

  def initTable(scn : MVL.SCNType): Unit = {
    columns = exec(getColumnList(table)).map{v => v._1 -> Column(v._1, v._2, v._3)}.toMap
    hasPK = exec(getIsPKLog(table)) match {
      case a : Vector[(String, String)] => a.head._2 == yesStr
      case _ => false
    }
    if (hasPK){
      consNamePK =  exec(getPKConstraintName(table)).head
      columnsPKLog = exec(getPKColumnName(this))
      columnsPK = columnsPKLog
    } else {
      columns = columns ++ Map(rowidStr-> Column(rowidStr, varchar2Str, shortNoStr))
      columnsPKLog = Vector(logRowStr)
      columnsPK = Vector(rowidStr)
    }
    val  schemaTableShortBuilder : SchemaBuilder = SchemaBuilder.struct
    val  schemaTableFullBuilder : SchemaBuilder = SchemaBuilder.struct
    columns.get(columnsPK.head).map(k => schemaTableShortBuilder.field(columnsPK.head, k.getSchema))
    columns.map(k => schemaTableFullBuilder.field(k._1, k._2.getSchema))
    schemaFull = schemaTableFullBuilder.optional().build()
    schemaShort = schemaTableShortBuilder.optional().build()
    initSCN = scn
  }

}
