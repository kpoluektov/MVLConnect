package org.mvlconnect
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.mvlconnect.model.{CDCTable, MVL}
import org.mvlconnect.schema.MVLSchema

trait CDCOper extends MVLSchema{
  def table: CDCTable
  def scn : MVL.SCNType
  def dmlType : String
  def schemaBefore : Schema
  def schemaAfter : Schema
  def addBefore() : Struct
  def addAfter() : Any
  def addSource(sMap:Map[String, String]) : Struct = {
    toStruct(
      sMap + (SCNStr -> scn)
      , getSourceSchema)
  }

  def addOp() : Map[String,Any] = {
    Map(operSchemaStr -> dmlType)
  }

  def processData(sch: Schema, sMap:Map[String, String]) : Struct = {
    val s: Struct = new Struct(sch)
    s.put(afterSchemaStr, addAfter())
    s.put(beforeSchemaStr, addBefore())
    s.put(sourceStr, addSource(sMap))
    addOp().map(k => s.put(k._1, k._2))
    s
  }
  def getSchema : Schema = {
    val  schemaBuilder : SchemaBuilder = SchemaBuilder.struct.name(table.toString)
    schemaBuilder.field(beforeSchemaStr, schemaBefore)
    schemaBuilder.field(afterSchemaStr, schemaAfter)
    schemaBuilder.field(operSchemaStr, Schema.STRING_SCHEMA)
    schemaBuilder.field(sourceStr, getSourceSchema)
    schemaBuilder.optional().build()
  }

  def toStruct(map: Map[String, Any], schema: Schema) : Struct = {
    val s: Struct = new Struct(schema)
    map.map(k => s.put(k._1, k._2))
    s
  }
}

case class CDCDelete[B](idName: String, idVal: B, schemaBefore: Schema)(tbl: CDCTable, scnValue: MVL.SCNType) extends CDCOper{
  val dmlType = deleteOpStr
  val scn = scnValue
  val table = tbl

  def addBefore= {
    toStruct(Map(idName -> idVal), schemaBefore)
  }
  val schemaAfter = SchemaBuilder.struct().optional().build()

  def addAfter= {
    null
  }
}

case class CDCInsert(schemaAfter: Schema, fields: Map[String,Any])(tbl: CDCTable, scnValue: MVL.SCNType) extends CDCOper{
  val dmlType = insertOpStr
  val scn = scnValue
  val table = tbl

  def addBefore= {
    null
  }

  val schemaBefore = SchemaBuilder.struct().optional().build()

  def addAfter= {
    toStruct(fields, schemaAfter)
  }
}

case class CDCUpdate(idName: String, schemaBefore: Schema, schemaAfter: Schema, fields: Map[String,Any])(tbl: CDCTable, scnValue: MVL.SCNType) extends CDCOper{
  val dmlType = updateOpStr
  val scn = scnValue
  val table = tbl

  def addBefore= {
    val key =  fields.get(idName) match{
      case v : Some[_] => v.get
      case x => x
    }
    toStruct(Map(idName -> key), schemaBefore)
  }

  def addAfter= {
    toStruct(fields, schemaAfter)
  }
}
