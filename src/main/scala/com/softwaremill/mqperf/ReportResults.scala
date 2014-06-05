package com.softwaremill.mqperf

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemRequest}
import scala.util.Random
import java.text.SimpleDateFormat
import java.util.Date

class ReportResults(testConfigName: String) extends DynamoResultsTable {

  def reportSendingComplete(start: Long, end: Long, msgsSent: Int) {
    doReport(start, end, msgsSent, typeReceive)
  }

  def reportReceivingComplete(start: Long, end: Long, msgsReceived: Int) {
    doReport(start, end, msgsReceived, typeReceive)
  }

  private def doReport(start: Long, end: Long, msgsCount: Int, _type: String) {
    val df = newDateFormat

    val testResultName = s"$testConfigName-${_type}-${Random.nextInt(100000)}"
    val took = (end - start).toString
    val startStr = new Date(start)
    val endStr = new Date(end)

    dynamoClient.putItem(new PutItemRequest()
      .withTableName(tableName)
      .addItemEntry(resultNameColumn, new AttributeValue(testResultName))
      .addItemEntry(msgsCountColumn, new AttributeValue().withN(msgsCount.toString))
      .addItemEntry(tookColumn, new AttributeValue().withN(took))
      .addItemEntry(startColumn, new AttributeValue(df.format(startStr)))
      .addItemEntry(endColumn, new AttributeValue(df.format(endStr)))
      .addItemEntry(typeColumn, new AttributeValue(_type))
    )

    println(s"$testResultName (${_type}, ${msgsCount.toString}): $took ($startStr -> $endStr")
  }

  private def newDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
}
