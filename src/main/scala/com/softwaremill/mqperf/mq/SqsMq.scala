package com.softwaremill.mqperf.mq

import java.util.concurrent.Executors

import com.amazonaws.ClientConfiguration
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import scala.collection.JavaConverters._
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, ReceiveMessageRequest, SendMessageBatchRequestEntry}
import com.softwaremill.mqperf.config.AWSCredentialsFromEnv
import com.amazonaws.regions.{Region, Regions}

class SqsMq(configMap: Map[String, String]) extends Mq {

  private val asyncBufferedClient = new ThreadLocal[AmazonSQSBufferedAsyncClient](){
    override def initialValue(): AmazonSQSBufferedAsyncClient = {
      val c = new AmazonSQSAsyncClient(AWSCredentialsFromEnv(), new ClientConfiguration() withMaxConnections 3,Executors.newFixedThreadPool(1))
      c.setRegion(Region.getRegion(Regions.US_EAST_1))
      c
      new AmazonSQSBufferedAsyncClient(c)
    }
  }

  private val queueUrl = asyncBufferedClient.get().createQueue("mqperf-test-queue").getQueueUrl

  override type MsgId = String

  override def createSender() = new MqSender {
    override def send(msgs: List[String]) = {
      asyncBufferedClient.get().sendMessageBatch(queueUrl,
        msgs.zipWithIndex.map { case (m, i) => new SendMessageBatchRequestEntry(i.toString, m)}.asJava
      )
    }
  }

  override def createReceiver() = new MqReceiver {
    override def receive(maxMsgCount: Int) = {
      asyncBufferedClient.get()
        .receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(maxMsgCount))
        .getMessages
        .asScala
        .map(m => (m.getReceiptHandle, m.getBody))
        .toList
    }

    override def ack(ids: List[MsgId]) = {
      ids.foreach { id => asyncBufferedClient.get().deleteMessageAsync(new DeleteMessageRequest(queueUrl, id))}
    }
  }
}
