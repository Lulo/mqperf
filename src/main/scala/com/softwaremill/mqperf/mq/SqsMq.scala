package com.softwaremill.mqperf.mq

import java.util.concurrent.Executors

import com.amazonaws.ClientConfiguration
import com.amazonaws.services.sqs.buffered.{QueueBufferConfig, AmazonSQSBufferedAsyncClient}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import scala.collection.JavaConverters._
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, ReceiveMessageRequest, SendMessageBatchRequestEntry}
import com.softwaremill.mqperf.config.AWSCredentialsFromEnv
import com.amazonaws.regions.{Region, Regions}

class SqsMq(configMap: Map[String, String]) extends Mq {
  private val asyncClient = {
    val c = new AmazonSQSAsyncClient(AWSCredentialsFromEnv())
    c.setRegion(Region.getRegion(Regions.US_EAST_1))
    c
  }

  private val asyncBufferedClient = new AmazonSQSBufferedAsyncClient(asyncClient, new QueueBufferConfig(200L, 5, 100, 100, true, 262143L, -1, 20, 10))

  private val queueUrl = asyncClient.createQueue("mqperf-test-queue").getQueueUrl

  override type MsgId = String

  override def createSender() = new MqSender {
    override def send(msgs: List[String]) = {
      asyncClient.sendMessageBatch(queueUrl,
        msgs.zipWithIndex.map { case (m, i) => new SendMessageBatchRequestEntry(i.toString, m)}.asJava
      )
    }
  }

  override def createReceiver() = new MqReceiver {
    override def receive(maxMsgCount: Int) = {
      asyncBufferedClient
        .receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(maxMsgCount))
        .getMessages
        .asScala
        .map(m => (m.getReceiptHandle, m.getBody))
        .toList
    }

    override def ack(ids: List[MsgId]) = {
      ids.foreach { id => asyncBufferedClient.deleteMessageAsync(new DeleteMessageRequest(queueUrl, id))}
    }
  }
}
