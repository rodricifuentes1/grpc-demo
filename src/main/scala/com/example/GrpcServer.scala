package com.example

import com.example.helloworld._
import com.example.userservice.{ GetUserRequest, GetUserResponse, GrpcUser, UserServiceGrpc }
import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ Server, ServerBuilder, ServerServiceDefinition }
import monix.execution.Scheduler

import scala.concurrent.{ ExecutionContext, Future }

object GrpcServer extends App with LazyLogging {

  val executionContext: ExecutionContext = ExecutionContext.global
  val monixScheduler: Scheduler          = Scheduler.global

  val helloService: ServerServiceDefinition =
    HelloWorldGrpc.bindService(new HelloWorldImpl()(executionContext), executionContext)

  val userService: ServerServiceDefinition =
    UserServiceGrpc.bindService(new UserServiceImpl()(monixScheduler), monixScheduler)

  val serverPort: Int = 9000
  val server: Server = ServerBuilder
    .forPort(serverPort)
    .addService(helloService)
    .addService(userService)
    .build()

  // start server
  server.start()
  logger.info(s"grpc server started in port $serverPort")

  server.awaitTermination()
}

class HelloWorldImpl()(implicit ec: ExecutionContext) extends HelloWorldGrpc.HelloWorld {
  val repo = new UserRepository()
  override def sayHello(request: HelloWorldRequest): Future[HelloWorldResponse] = {
    Future(HelloWorldResponse(s"Hello to you ${request.name}"))
  }
}

class UserServiceImpl()(implicit scheduler: Scheduler) extends UserServiceGrpc.UserService {
  val repo = new UserRepository()
  override def getUser(request: GetUserRequest): Future[GetUserResponse] = {
    repo
      .getUserTask(request.userId)
      .map(user => GetUserResponse(user = Some(GrpcUser(id = user.id, name = user.name))))
      .runAsync
  }
}
