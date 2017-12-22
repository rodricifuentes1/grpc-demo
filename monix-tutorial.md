### Monix use case
We need to create a new GRPC service that given an student id returns a student with its complete list of grades!

#### Steps:
1. Create a Student class with an Id and list of grades (optional)
```scala
case class Student(id: Int, grades: Option[List[Double]])
```

2. Create a scala DAO that "connects" to a external data source to look for:
- A student given an Id
`def getStudent(id: Int)(implicit ec: ExecutionContext): Future[Student]`

- A student grade given an id
`def getStudentGrades(studentId: Int)(implicit ec: ExecutionContext): List[Double]`

```scala
package com.example

import scala.concurrent.{ ExecutionContext, Future }

class StudentsDAO() {
  def getStudent(id: Int)(implicit ec: ExecutionContext): Future[Student] = Future {
    Student(id, None)
  }

  def getStudentGrades(studentId: Int)(implicit ec: ExecutionContext): Future[List[Double]] = Future {
    if (studentId == 1) List(5.0, 5.0, 4.8, 5.0, 3.3)
    else List(4.0, 3.7, 5.0, 1.2, 0.0, 4.3)
  }
}
```

3. Create a scala Repository that calls exposed services in the DAO and returns the user using a monix task
```scala
package com.example

import monix.eval.Task

class StudentsRepo() {

  private lazy val dao = new StudentsDAO()

  def getStudent(id: Int): Task[Student] = {
    // methods dao.getStudent and dao.getStudent grades need an implicit execution context (because they use futures)
    // Task.deferFutureAction provides task scheduler (monix execution context) and passes it into dao methods
    val getStudentTask       = Task.deferFutureAction(implicit scheduler => dao.getStudent(id))
    val getStudentGradesTask = Task.deferFutureAction(implicit scheduler => dao.getStudentGrades(id))
    for {
      student <- getStudentTask
      grades  <- getStudentGradesTask
    } yield student.copy(grades = Some(grades))
  }
}
```

4. Create a new students gpc-service to that queries one studen given its id

- Create proto file (messages and service)
```
// protocol version 3
syntax = "proto3";

// package
package com.example;

// -----------------
// protobuf messages
// -----------------

// request message
message GetStudentRequest {
    string id = 1;
}

// response message
message StudentProto {
    int32 id = 1;
    repeated double grades = 2;
}

message GetStudentResponse {
    string message = 1;
    StudentProto student = 2;
}

// -------------
// grpc-services
// -------------
service Students {
    rpc getStudent(GetStudentRequest) returns (GetStudentResponse);
}
```

- Create the grpc-service implementation
```scala
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
```

- Bind students grpc-service into grpc-server
```scala
  // create monix scheduler
  val monixScheduler = Scheduler.global
  // create service definition
  val studentsService: ServerServiceDefinition =StudentsGrpc.bindService(new StudentsServiceImpl(monixScheduler), executionContext)
  // bind service into grpc-server
  val server: Server = ServerBuilder
      .addService(studentsService)
      .build()
```

5. Create grpc-client
```scala
class StudentsClient(serverChannel: ManagedChannel) extends LazyLogging {
  private val asyncStub    = StudentsGrpc.stub(serverChannel)
  
  def getStudent(id: Int): Future[GetStudentResponse] = {
    logger.info("calling say hello (async)")
    val request = GetStudentRequest(id)
    asyncStub.getStudent(request)
  }
}
```

6. Use the client
```scala
  val studentsClient: StudentsClient = new StudentsClient(serverChannel)
  val getStudentAsync                = studentsClient.getStudent(1)
  val getStudentAsyncResponse        = Await.result(getStudentAsync, 10.seconds)
  logger.info(s"GET STUDENT ASYNC RESPONSE $getStudentAsyncResponse")
```