package org.mvlconnect.connector

import java.util

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import com.typesafe.scalalogging.Logger
import org.apache.kafka.connect.source.{SourceRecord, SourceTask}
import org.mvlconnect.schema.MVLSchema
import org.mvlconnect.{DBUtils, StreamTable, Verifier}
import org.mvlconnect.model.{CDCData, CDCEmpty, MVL, SCNRange}
import org.slf4j.LoggerFactory

import collection.JavaConverters._
import scala.concurrent.duration._
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Success, Try}

class MVLTask extends SourceTask with Constants with MVLSchema with DBUtils{

  implicit val actorSystem : ActorSystem = ActorSystem("MVLTask")

  val log: Logger = Logger(LoggerFactory.getLogger(this.getClass))
  var mQueue : mutable.Queue[Map[String,Any]] = mutable.Queue()
  implicit val executor: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  var tables : Array[StreamTable] = Array()
  var topic  = "mvl"
  var processChanges : Boolean = false
  var flashbackQuery : Boolean = true
  var dbTimeout = 10
  var sourceMap : Map[String, String] = Map()


  override def start(props: util.Map[String, String]): Unit = {
    log.info("MVLTask starting..")
    log.debug("Task props is {}", props.toString)
    topic = props.get(topicStr)
    processChanges = props.get(processStr).toBoolean
    flashbackQuery = props.get(flashbackQueryStr).toBoolean
    dbTimeout = props.get(timeoutStr).toInt
    val CDCTables = Verifier.splitConf2Tables(props.get("tables"))
    tables = CDCTables.map(t=> new StreamTable(t))
    if (Verifier.verify(tables)) {
      log.info("tables list verified")
    } else{
      log.error("tables list unverified")
    }
    tables.foreach(t => {
      val offset = context.offsetStorageReader().offset(t.table.getPartition)
      val s  = Try(offset.get(SCNStr).asInstanceOf[MVL.SCNType]) match {
        case Success(result) => result
        case Failure(_) => 0.asInstanceOf[MVL.SCNType]
      }
      log.debug("SCN {} inited for table {}", s.toString, t.table)
      t.initTable(s)
    }
    )
    tables.zipWithIndex.foreach{case (element, index) => element.actor = Some(actorSystem.actorOf(Props(new CDCActor(element)), "CDCActor" +index ))}
    // fill scheme source map
    sourceMap += (
      connectorStr -> props.get(connectorClass),
      versionStr -> ver,
//      tableStr -> ver,
      dbStr -> props.get(configDBStr + "." + urlStr),
      schemaStr -> props.get(configDBStr + "." + userStr)
    )
    log.debug("sourceMap is {}", sourceMap)
    log.info("MVLTask started successfully")
  }

  override def poll(): util.List[SourceRecord] = {
    implicit val timeout: Timeout = Timeout(dbTimeout seconds)
    var records : List[SourceRecord] = List()
    val curSCN = getCurrentSCNValue
    for (tab <- tables) {
      val future = tab.actor.get ? SCNRange(tab.initSCN, curSCN, processChanges, flashbackQuery)
      val res = Await.result(future, timeout.duration)
      res match {
        case x: CDCData =>
          log.debug(s"got CDCData. {}", x)
          x.rows.foreach(k =>  records = new SourceRecord(
                                                        tab.table.getPartition,
                                                        x.getOffSet,
                                                        topic,
                                                        k.getSchema,
                                                        k.processData(
                                                            k.getSchema,
                                                            sourceMap + (tableStr -> x.table.getNameOnly)
                                                        )
          ) :: records)
          log.debug(s"initXID on exit is {}", tab.initSCN)
        case _: CDCEmpty => log.debug(s"got CDCEmpty. Assiming no changes there")
        case _ => log.error(s"got null. Unknown answer")
      }
      tab.initSCN = curSCN
    }
    records.asJava
  }

  override def stop(): Unit = {
    actorSystem.terminate()
  }

  override def version(): String = ver

}
