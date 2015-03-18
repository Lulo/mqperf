package com.softwaremill.mqperf

import java.util.concurrent.atomic.AtomicLong

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemRequest}
import scala.util.Random
import java.text.SimpleDateFormat
import java.util.Date
import com.softwaremill.mqperf.util.Retry._

class ReportResults(testConfigName: String) {

  protected val typeSend = "s"
  protected val typeReceive = "r"
  val total = new AtomicLong()
  def reportSendingComplete(start: Long, end: Long, msgsSent: Int) {
    tryDoReport(start, end, msgsSent, typeSend)
  }

  def reportReceivingComplete(start: Long, end: Long, msgsReceived: Int) {
    tryDoReport(start, end, msgsReceived, typeReceive)
  }

  private def tryDoReport(start: Long, end: Long, msgsCount: Int, _type: String) {
    retry(10, () => Thread.sleep(1000L)) {
      doReport(start, end, msgsCount, _type)
    }
  }

  private def doReport(start: Long, end: Long, msgsCount: Int, _type: String) {
    val df = newDateFormat

    val testResultName = s"$testConfigName-${_type}-${Random.nextInt(100000)}"
    val took = (end - start).toString
    val startStr = new Date(start)
    val endStr = new Date(end)

    println(s"$testResultName (${_type}, $msgsCount): $took total:${total.addAndGet(msgsCount)}")
  }

  private def newDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
}

