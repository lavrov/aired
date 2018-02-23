package com.github.lavrov.aired.webservice.utils

import io.getquill.{CassandraAsyncContext, CassandraContextConfig, CassandraStreamContext, SnakeCase}

class CassandraSession(cassandraContextConfig: CassandraContextConfig) {
  val async = new CassandraAsyncContext(SnakeCase, cassandraContextConfig)
  val streaming = new CassandraStreamContext(SnakeCase, cassandraContextConfig)
}
