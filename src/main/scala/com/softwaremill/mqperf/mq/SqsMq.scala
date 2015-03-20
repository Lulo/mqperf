package com.softwaremill.mqperf.mq

import java.lang.ThreadLocal
import java.util.concurrent.Executors

import com.amazonaws.ClientConfiguration
import com.amazonaws.services.sqs.buffered.{QueueBufferConfig, AmazonSQSBufferedAsyncClient}
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClient, AmazonSQSAsyncClient}
import scala.collection.JavaConverters._
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, ReceiveMessageRequest, SendMessageBatchRequestEntry}
import com.softwaremill.mqperf.config.AWSCredentialsFromEnv
import com.amazonaws.regions.{Region, Regions}

import scala.concurrent.Future

class SqsMq(configMap: Map[String, String]) extends Mq {
  def asyncClient = asyncBufferedClient

  def asyncBufferedClient =
    asyncBufferedClientVal.get()
/*

  val asyncBufferedClient =
   createClient()
*/

  def createClient() = {
    /*val asyncClient = {
      val c = new AmazonSQSAsyncClient(AWSCredentialsFromEnv(), new ClientConfiguration() withMaxConnections 1000, Executors.newCachedThreadPool())
      c.setRegion(Region.getRegion(Regions.US_EAST_1))
      c
    }
    new AmazonSQSBufferedAsyncClient(asyncClient ,
    new QueueBufferConfig(200L, 5, 1000, 1000, true, 262143L, -1, 20, 10)
    )*/
    {
      val c = new AmazonSQSClient(AWSCredentialsFromEnv(), new ClientConfiguration() withMaxConnections 4)
      c.setRegion(Region.getRegion(Regions.US_EAST_1))
      c
    }
  }

  val syncClient = new AmazonSQSClient(AWSCredentialsFromEnv())
  private lazy val asyncBufferedClientVal = new ThreadLocal[AmazonSQS] {

    override def initialValue(): AmazonSQS = createClient()
  }

  private val queueUrl = asyncClient.createQueue("mqperf-test-queue").getQueueUrl

  override type MsgId = String

  override def createSender() = new MqSender {
    override def send(msg: String) = {
      asyncClient.sendMessageBatch(queueUrl,
        (1 to 10).map { i => new SendMessageBatchRequestEntry(i.toString, msg)}.asJava
      )
      10
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
      /*ids.foreach { id => Future {
        asyncBufferedClient.deleteMessage(new DeleteMessageRequest(queueUrl, id))
      }}*/
    }
  }
}
