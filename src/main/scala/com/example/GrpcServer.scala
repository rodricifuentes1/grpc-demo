package com.example

import com.example.helloworld._
import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ Server, ServerBuilder, ServerServiceDefinition }
import monix.execution.Scheduler

import scala.concurrent.{ ExecutionContext, Future }

object GrpcServer extends App with LazyLogging {
  val monixScheduler: Scheduler = Scheduler.global
  val helloService: ServerServiceDefinition =
    HelloWorldGrpc.bindService(new HelloWorldImpl()(monixScheduler), ExecutionContext.global)
  val serverPort: Int = 9000
  val server: Server = ServerBuilder
    .forPort(serverPort)
    .addService(helloService)
    .build()

  // start server
  server.start()
  logger.info(s"grpc server started in port $serverPort")

  server.awaitTermination()
}

class HelloWorldImpl()(implicit scheduler: Scheduler) extends HelloWorldGrpc.HelloWorld {
  val repo = new UserRepository()

  override def sayHello(request: HelloWorldRequest): Future[HelloWorldResponse] = {
    Future(HelloWorldResponse(s"Hello to you ${request.name}"))
  }

  override def getUser(request: GetUserRequest): Future[GetUserResponse] = {
    repo
      .getUserTask(request.userId)
      .map(user => GetUserResponse(user = Some(GrpcUser(id = user.id, name = user.name))))
      .runAsync
  }
}
