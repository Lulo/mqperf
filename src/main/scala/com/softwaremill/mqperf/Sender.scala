package com.softwaremill.mqperf

import com.softwaremill.mqperf.config.TestConfig
import com.softwaremill.mqperf.mq.Mq

import scala.util.Random

object Sender extends App {
  for (numThreads <- Seq(20,50,75)) {
    val testConfig =  TestConfig(
      name=s"sqs-$numThreads",
      mqType = "Sqs",
      senderThreads = 75,
      msgSize =50,
      maxSendMsgBatchSize = 10,
      receiverThreads = numThreads,
      receiveMsgBatchSize = 10,
      mqConfigMap = Map.empty)
    println(s"Starting test (sender) with config: $testConfig")

    val mq = Mq.instantiate(testConfig)
    val report = new ReportResults(testConfig.name)
    val sr = new SenderRunnable(
      mq, report,
      "0" * testConfig.msgSize,
      testConfig.maxSendMsgBatchSize
    )

    val threads = (1 to testConfig.senderThreads).map { _ =>
      val t = new Thread(sr)
      t.start()
      t
    }

    threads.foreach(_.join())

    mq.close()
  }
}

class SenderRunnable(mq: Mq, reportResults: ReportResults,
  msg: String, maxSendMsgBatchSize: Int) extends Runnable {

  override def run() = {
    val mqSender = mq.createSender()
    val start = System.currentTimeMillis()
    var totalSent = 0

    while ((System.currentTimeMillis() - start) < 60*1000L) {
      val received = doSend(mqSender)
      totalSent += received
    }

    val end = System.currentTimeMillis()
    reportResults.reportSendingComplete(start, end, totalSent)
    mqSender.close()
  }

  private def doSend(mqSender: mq.MqSender)= {
    mqSender.send(msg)
  }
}