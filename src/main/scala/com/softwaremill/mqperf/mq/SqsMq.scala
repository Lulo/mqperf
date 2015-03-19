package com.softwaremill.mqperf.mq

import java.lang.ThreadLocal
import java.util.concurrent.Executors

import com.amazonaws.ClientConfiguration
import com.amazonaws.services.sqs.buffered.{QueueBufferConfig, AmazonSQSBufferedAsyncClient}
import com.amazonaws.services.sqs.{AmazonSQSClient, AmazonSQSAsyncClient}
import scala.collection.JavaConverters._
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, ReceiveMessageRequest, SendMessageBatchRequestEntry}
import com.softwaremill.mqperf.config.AWSCredentialsFromEnv
import com.amazonaws.regions.{Region, Regions}

class SqsMq(configMap: Map[String, String]) extends Mq {
  def asyncClient = asyncBufferedClient

/*
  def asyncBufferedClient =
    asyncBufferedClientVal.get()
*/

  val asyncBufferedClient =
   createClient()

  def createClient() = {
    val asyncClient = {
      val c = new AmazonSQSAsyncClient(AWSCredentialsFromEnv(), new ClientConfiguration() withMaxConnections 4, Executors.newFixedThreadPool(1))
      c.setRegion(Region.getRegion(Regions.US_EAST_1))
      c
    }
    new AmazonSQSBufferedAsyncClient(asyncClient)
  }

  val syncClient = new AmazonSQSClient(AWSCredentialsFromEnv())
/*

  private lazy val asyncBufferedClientVal = new ThreadLocal[AmazonSQSBufferedAsyncClient] {

    override def initialValue(): AmazonSQSBufferedAsyncClient = createClient()
  }
*/

  private val queueUrl = asyncClient.createQueue("mqperf-test-queue").getQueueUrl

  override type MsgId = String

  override def createSender() = new MqSender {
    override def send(msg: String) = {
      syncClient.sendMessage(queueUrl, msg)
      1
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
