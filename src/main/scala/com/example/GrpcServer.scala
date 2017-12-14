package com.example

import com.example.helloworld.{ HelloWorldGrpc, HelloWorldRequest, HelloWorldResponse }
import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ Server, ServerBuilder, ServerServiceDefinition }

import scala.concurrent.{ ExecutionContext, Future }

object GrpcServer extends App with LazyLogging {
  val executionContext = ExecutionContext.global
  val helloService: ServerServiceDefinition =
    HelloWorldGrpc.bindService(new HelloWorldImpl()(executionContext), executionContext)
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

class HelloWorldImpl()(implicit ec: ExecutionContext) extends HelloWorldGrpc.HelloWorld {
  override def sayHello(request: HelloWorldRequest): Future[HelloWorldResponse] = {
    Future(HelloWorldResponse(s"Hello to you ${request.name}"))
  }
}
