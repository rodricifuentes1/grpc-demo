package com.example

import com.example.helloworld.{ HelloWorldGrpc, HelloWorldRequest, HelloWorldResponse }
import com.example.students.{ GetStudentRequest, GetStudentResponse, StudentsGrpc }
import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ ManagedChannel, ManagedChannelBuilder }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object GrpcClient extends App with LazyLogging {
  val serverAddress: String = "localhost"
  val serverPort: Int       = 9000
  val serverChannel: ManagedChannel = ManagedChannelBuilder
    .forAddress(serverAddress, serverPort)
    .usePlaintext(true)
    .build()

  // client instance
  val helloWorldClient: HelloWorldClient = new HelloWorldClient(serverChannel)

  // blocking call
  val blockingCall: HelloWorldResponse = helloWorldClient.sayHelloBlocking("BLOCKING")
  logger.info(s"BLOCKING RESPONSE $blockingCall")

  // async call

  // this is going to be executed in another thread
  val asyncCall: Future[HelloWorldResponse] = helloWorldClient.sayHelloAsync("ASYNC")
  // we should never do this
  // we're going to await for async response ONLY for demo purpose
  val asyncResponse: HelloWorldResponse = Await.result(asyncCall, 10.seconds)
  logger.info(s"ASYNC RESPONSE $asyncResponse")

  val studentsClient: StudentsClient = new StudentsClient(serverChannel)
  val getStudentAsync                = studentsClient.getStudent(1)
  val getStudentAsyncResponse        = Await.result(getStudentAsync, 10.seconds)
  logger.info(s"GET STUDENT ASYNC RESPONSE $getStudentAsyncResponse")
}

class HelloWorldClient(serverChannel: ManagedChannel) extends LazyLogging {

  private val asyncStub    = HelloWorldGrpc.stub(serverChannel)
  private val blockingStub = HelloWorldGrpc.blockingStub(serverChannel)

  def sayHelloAsync(name: String): Future[HelloWorldResponse] = {
    logger.info("calling say hello (async)")
    val request = HelloWorldRequest(name)
    asyncStub.sayHello(request)
  }

  def sayHelloBlocking(name: String): HelloWorldResponse = {
    logger.info("calling say hello (blocking)")
    val request = HelloWorldRequest(name)
    blockingStub.sayHello(request)
  }
}

class StudentsClient(serverChannel: ManagedChannel) extends LazyLogging {
  private val asyncStub = StudentsGrpc.stub(serverChannel)

  def getStudent(id: Int): Future[GetStudentResponse] = {
    logger.info("calling say hello (async)")
    val request = GetStudentRequest(id)
    asyncStub.getStudent(request)
  }
}
