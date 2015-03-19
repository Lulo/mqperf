package com.softwaremill.mqperf

import com.softwaremill.mqperf.config.TestConfig
import com.softwaremill.mqperf.mq.Mq

object Receiver extends App {
  def testConfigWithThreads(numThreads:Int) = TestConfig(
                         name=s"sqs-$numThreads",
                         mqType = "Sqs",
                         senderThreads = 0,
                         msgCountPerThread =0,
                         msgSize =50,
                         maxSendMsgBatchSize = 0,
                         receiverThreads = numThreads,
                         receiveMsgBatchSize = 10,
                         mqConfigMap = Map.empty)

  for (numThreads <- Seq(1, 10, 100)) {
    val testConfig = testConfigWithThreads(numThreads)
    println(s"Starting test (receiver) with config: $testConfig")

    val mq = Mq.instantiate(testConfig)
    val report = new ReportResults(testConfig.name)
    val rr = new ReceiverRunnable(
      mq, report,
      testConfig.receiveMsgBatchSize
    )

    val threads = (1 to testConfig.receiverThreads).map { _ =>
      val t = new Thread(rr)
      t.start()
      t
    }

    threads.foreach(_.join())

    mq.close()
  }
}

class ReceiverRunnable(
  mq: Mq,
  reportResults: ReportResults,
  receiveMsgBatchSize: Int) extends Runnable {

  override def run() = {
    val mqReceiver = mq.createReceiver()

    val start = System.currentTimeMillis()
    var totalReceived = 0

    while ((System.currentTimeMillis() - start) < 60*1000L) {
      val received = doReceive(mqReceiver)
      totalReceived += received
    }

    reportResults.reportReceivingComplete(start, System.currentTimeMillis(), totalReceived)
    mqReceiver.close()
  }

  private def doReceive(mqReceiver: mq.MqReceiver) = {
    val msgs = mqReceiver.receive(receiveMsgBatchSize)
    val ids = msgs.map(_._1)
    if (ids.size > 0) {
      mqReceiver.ack(ids)
    }

    ids.size
  }
}