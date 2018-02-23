package com.github.lavrov.aired.webservice

import java.util.Scanner

import io.getquill.CassandraContextConfig
import org.slf4j.LoggerFactory

class CassandraBootstrap(cassandraContextConfig: CassandraContextConfig) {
  val logger = LoggerFactory getLogger getClass

  def schema() = {
    val session = cassandraContextConfig.cluster.connect()
    val scanner = new Scanner(getClass.getClassLoader.getResourceAsStream("schema.cql"))
    scanner.useDelimiter(";")
    Iterator.continually((scanner.hasNext(), scanner.next()))
      .takeWhile {
        case (hasNext, statement) => hasNext && statement.trim.nonEmpty
      }
      .map(_._2)
      .foreach { statement =>
        logger.info(s"Executing: \n$statement")
        session.execute(statement)
      }
  }

}
