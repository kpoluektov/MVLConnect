package org.mvlconnect.connector

import java.util

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector
import org.apache.kafka.connect.util.ConnectorUtils
import org.mvlconnect.model.CDCTable
import org.mvlconnect.{DBUtils, Verifier}
import org.slf4j.LoggerFactory

import collection.JavaConverters._
import scala.collection.immutable.HashMap

class MVLConnector extends SourceConnector with Constants with DBUtils {
  val log = Logger(LoggerFactory.getLogger(this.getClass))
  var tables : Array[CDCTable] = Array()
  var topic : String = ""
  var processChanges : Boolean = false
  var flashbackQuery : Boolean = true
  var dbTimeout = 10
  var dbUrl : String = ""
  var dbUser : String = ""

  override def start(props: util.Map[String, String]): Unit = {
    val config : Config = ConfigFactory.parseMap(props)
    if(init(config))
      log.info("MVLConnector started successfully")
    else
      log.error("MVLConnector. Connection error")
    tables = Verifier.splitConf2Tables(props.get("tables"))
    topic = props.get(topicStr)
    processChanges = props.get(processStr).toBoolean
    flashbackQuery = props.get(flashbackQueryStr).toBoolean
    dbTimeout = props.get(timeoutStr).toInt
    dbUrl = props.get(configDBStr + "." + urlStr)
    dbUser = props.get(configDBStr + "." + userStr)
  }

  override def taskClass(): Class[_ <: Task] = classOf[MVLTask]

  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] = {
    val numGroups: Int = Math.min(tables.size, maxTasks)
    val groupedTables = ConnectorUtils.groupPartitions(tables.toList.asJava, numGroups).asScala
    var taskConfigs: List[util.Map[String, String]] = List()
    for (taskTables <- groupedTables) {
      val ta: Map[String, String] = HashMap("tables" -> taskTables.asScala.mkString(","),
        topicStr-> topic,
        processStr -> processChanges.toString,
        flashbackQueryStr -> flashbackQuery.toString,
        timeoutStr -> dbTimeout.toString,
        configDBStr + "." + urlStr -> dbUrl,
        configDBStr + "." + userStr -> dbUser,
        connectorClass -> this.getClass.getName
      )
      taskConfigs = ta.asJava :: taskConfigs
      log.info("adding config to task {}", ta.toString())
    }
    taskConfigs.asJava
  }

  override def stop(): Unit = {
    DBClose
  }

  override def config(): ConfigDef = {new ConfigDef()}

  override def version(): String = ver
}
