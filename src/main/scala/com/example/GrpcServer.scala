package com.example

import com.example.helloworld.{ HelloWorldGrpc, HelloWorldRequest, HelloWorldResponse }
import com.example.students.{ GetStudentRequest, GetStudentResponse, StudentProto, StudentsGrpc }
import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ Server, ServerBuilder, ServerServiceDefinition }
import monix.execution.Scheduler

import scala.concurrent.{ ExecutionContext, Future }

object GrpcServer extends App with LazyLogging {
  val executionContext = ExecutionContext.global
  val helloService: ServerServiceDefinition =
    HelloWorldGrpc.bindService(new HelloWorldImpl()(executionContext), executionContext)

  // create monix scheduler
  val monixScheduler = Scheduler.global

  // create service definition
  val studentsService: ServerServiceDefinition =
    StudentsGrpc.bindService(new StudentsServiceImpl(monixScheduler), executionContext)
  val serverPort: Int = 9000
  val server: Server = ServerBuilder
    .forPort(serverPort)
    .addService(helloService)
    .addService(studentsService)
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

class StudentsServiceImpl(scheduler: Scheduler) extends StudentsGrpc.Students {
  implicit val monixScheduler = scheduler
  private lazy val repo       = new StudentsRepo()

  override def getStudent(request: GetStudentRequest): Future[GetStudentResponse] = {
    repo
      .getStudent(request.id)
      .map(student => GetStudentResponse(student = Some(StudentProto(student.id, student.grades.get))))
      .runAsync
  }
}
