package com.softwaremill.mqperf.config

case class TestConfig(
  name: String,
  mqType: String,
  senderThreads: Int,
  msgCountPerThread: Int,
  msgSize: Int,
  maxSendMsgBatchSize: Int,
  receiverThreads: Int,
  receiveMsgBatchSize: Int,
  mqConfigMap: Map[String, String]) {

  def mqClassName = s"com.softwaremill.mqperf.mq.${mqType}Mq"
}
